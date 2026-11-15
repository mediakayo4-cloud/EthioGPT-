package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.EthioAiService
import com.example.data.local.AppPreferences
import com.example.data.model.AiToolMode
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.EthioLanguage
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.example.data.repository.EthioGptRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class EthioGptViewModel(
    private val repository: EthioGptRepository,
    private val aiService: EthioAiService,
    private val preferences: AppPreferences,
    val currentUserId: String,
    initialDisplayName: String,
    initialEmail: String?
) : ViewModel() {

    val userProfileState: StateFlow<DataUiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, DataUiState<UserProfile?>> { profile ->
                if (profile != null) {
                    syncProfilePreferences(profile)
                }
                DataUiState.Success(profile)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing user profile", rootError)
                emit(DataUiState.Error(rootError.message ?: "Failed to load profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = DataUiState.Loading
            )

    val conversationsState: StateFlow<DataUiState<List<Conversation>>> =
        repository.observeConversations(currentUserId)
            .map<List<Conversation>, DataUiState<List<Conversation>>> { DataUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing conversations", rootError)
                emit(DataUiState.Error(rootError.message ?: "Failed to load chat history"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = DataUiState.Loading
            )

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val activeMessages: StateFlow<List<ChatMessage>> =
        _activeConversationId
            .flatMapLatest { convId ->
                if (convId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    repository.observeMessages(currentUserId, convId)
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            Log.w(TAG, "Error observing messages for $convId", firestoreError ?: error)
                            emit(emptyList())
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    private val _selectedLanguage = MutableStateFlow(EthioLanguage.ENGLISH)
    val selectedLanguage: StateFlow<EthioLanguage> = _selectedLanguage.asStateFlow()

    private val _selectedToolMode = MutableStateFlow(AiToolMode.GENERAL)
    val selectedToolMode: StateFlow<AiToolMode> = _selectedToolMode.asStateFlow()

    private val _composerText = MutableStateFlow("")
    val composerText: StateFlow<String> = _composerText.asStateFlow()

    private val _pendingImageUri = MutableStateFlow<Uri?>(null)
    val pendingImageUri: StateFlow<Uri?> = _pendingImageUri.asStateFlow()

    private val _pendingImageBitmap = MutableStateFlow<Bitmap?>(null)
    val pendingImageBitmap: StateFlow<Bitmap?> = _pendingImageBitmap.asStateFlow()

    private val _pendingUserMessage = MutableStateFlow<ChatMessage?>(null)
    val pendingUserMessage: StateFlow<ChatMessage?> = _pendingUserMessage.asStateFlow()

    private val _streamingReplyText = MutableStateFlow<String?>(null)
    val streamingReplyText: StateFlow<String?> = _streamingReplyText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _historyToolFilter = MutableStateFlow<AiToolMode?>(null)
    val historyToolFilter: StateFlow<AiToolMode?> = _historyToolFilter.asStateFlow()

    private var profileSyncedOnce = false

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                displayName = initialDisplayName,
                email = initialEmail
            )
        }
    }

    private suspend fun syncProfilePreferences(profile: UserProfile) {
        if (!profileSyncedOnce) {
            profileSyncedOnce = true
            val lang = EthioLanguage.fromCode(profile.preferredLanguage)
            val theme = ThemeMode.fromCode(profile.themeMode)
            _selectedLanguage.value = lang
            preferences.setDefaultLanguage(lang)
            preferences.setThemeMode(theme)
            preferences.setVoiceAutoSpeak(profile.voiceAutoSpeak)
        }
    }

    fun updateComposerText(text: String) {
        _composerText.value = text
    }

    fun attachImage(uri: Uri?, bitmap: Bitmap?) {
        _pendingImageUri.value = uri
        _pendingImageBitmap.value = bitmap
    }

    fun clearAttachedImage() {
        _pendingImageUri.value = null
        _pendingImageBitmap.value = null
    }

    fun setLanguage(language: EthioLanguage, persistAsDefault: Boolean = false) {
        _selectedLanguage.value = language
        if (persistAsDefault) {
            viewModelScope.launch {
                preferences.setDefaultLanguage(language)
                repository.updateUserPreferences(preferredLanguage = language.code)
            }
        }
    }

    fun setToolMode(toolMode: AiToolMode) {
        _selectedToolMode.value = toolMode
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(themeMode)
            repository.updateUserPreferences(themeMode = themeMode.code)
        }
    }

    fun setVoiceAutoSpeak(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setVoiceAutoSpeak(enabled)
            repository.updateUserPreferences(voiceAutoSpeak = enabled)
        }
    }

    fun updateDisplayName(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateUserPreferences(displayName = newName)
        }
    }

    fun startNewChat(
        toolMode: AiToolMode = AiToolMode.GENERAL,
        language: EthioLanguage = _selectedLanguage.value,
        prefillPrompt: String = ""
    ) {
        _activeConversationId.value = null
        _selectedToolMode.value = toolMode
        _selectedLanguage.value = language
        _composerText.value = prefillPrompt
        _pendingUserMessage.value = null
        _streamingReplyText.value = null
        _chatError.value = null
        clearAttachedImage()
    }

    fun openConversation(conversation: Conversation) {
        _activeConversationId.value = conversation.id
        _selectedToolMode.value = AiToolMode.fromCode(conversation.toolMode)
        _selectedLanguage.value = EthioLanguage.fromCode(conversation.language)
        _pendingUserMessage.value = null
        _streamingReplyText.value = null
        _chatError.value = null
        clearAttachedImage()
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = null
            }
            repository.deleteConversation(conversationId)
        }
    }

    fun updateHistorySearch(query: String) {
        _historySearchQuery.value = query
    }

    fun updateHistoryFilter(toolMode: AiToolMode?) {
        _historyToolFilter.value = toolMode
    }

    fun clearChatError() {
        _chatError.value = null
    }

    fun sendMessage(
        overridePrompt: String? = null,
        onReplyCompleted: (messageId: String, replyText: String, language: EthioLanguage) -> Unit = { _, _, _ -> }
    ) {
        if (_isGenerating.value) return
        val promptText = (overridePrompt ?: _composerText.value).trim()
        val imageBitmap = _pendingImageBitmap.value
        if (promptText.isBlank() && imageBitmap == null) return

        val language = _selectedLanguage.value
        val toolMode = _selectedToolMode.value
        val currentConvId = _activeConversationId.value
        val historySnapshot = activeMessages.value

        val effectiveUserDisplay = if (promptText.isNotBlank()) promptText else "Analyze this image"

        _composerText.value = ""
        clearAttachedImage()
        _chatError.value = null
        _isGenerating.value = true
        _pendingUserMessage.value = ChatMessage(
            id = "pending_user",
            userId = currentUserId,
            role = "user",
            text = effectiveUserDisplay,
            toolMode = toolMode.code,
            hasImage = imageBitmap != null
        )
        _streamingReplyText.value = ""

        viewModelScope.launch {
            var finalReply = ""
            try {
                withTimeout(AI_TIMEOUT_MS) {
                    aiService.streamAssistantReply(
                        historyMessages = historySnapshot,
                        userPrompt = promptText,
                        attachedImage = imageBitmap,
                        toolMode = toolMode,
                        language = language
                    ).collect { partial ->
                        finalReply = partial
                        _streamingReplyText.value = partial
                    }
                }

                if (finalReply.isBlank()) {
                    _chatError.value = "EthioGPT returned an empty response. Please try again."
                    _pendingUserMessage.value = null
                    _streamingReplyText.value = null
                    _isGenerating.value = false
                    return@launch
                }

                val titleCandidate = effectiveUserDisplay
                    .replace('\n', ' ')
                    .take(48)
                    .ifBlank { toolMode.title }

                val saveResult = repository.saveExchange(
                    conversationId = currentConvId,
                    title = titleCandidate,
                    toolMode = toolMode.code,
                    language = language.code,
                    userText = effectiveUserDisplay,
                    modelText = finalReply,
                    hasImage = imageBitmap != null
                )

                saveResult.onSuccess { savedConvId ->
                    if (currentConvId == null) {
                        _activeConversationId.value = savedConvId
                    }
                    _pendingUserMessage.value = null
                    _streamingReplyText.value = null
                    onReplyCompleted("reply_${System.currentTimeMillis()}", finalReply, language)
                }.onFailure { err ->
                    Log.e(TAG, "Failed to persist chat exchange", err)
                    _chatError.value = "Reply received, but failed to save to cloud history: ${err.localizedMessage}"
                    _pendingUserMessage.value = null
                    _streamingReplyText.value = null
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI generation timed out", e)
                _chatError.value = "Request timed out after 60 seconds. Please check your connection and try again."
                _pendingUserMessage.value = null
                _streamingReplyText.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                _chatError.value = e.localizedMessage ?: "Failed to generate response. Please try again."
                _pendingUserMessage.value = null
                _streamingReplyText.value = null
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private companion object {
        const val TAG = "EthioGptVM"
        const val AI_TIMEOUT_MS = 60_000L
    }
}
