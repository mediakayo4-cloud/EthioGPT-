package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.VoiceAssistantManager
import com.example.data.ai.EthioAiService
import com.example.data.model.AiToolMode
import com.example.data.model.ChatMessage
import com.example.data.model.EthioLanguage
import com.example.ui.components.EthioLogoBadge
import com.example.ui.components.EthioTriColorStripe
import com.example.ui.components.FormattedAiText
import com.example.ui.components.ToolModePill
import com.example.ui.theme.NotoEthiopicFontFamily
import com.example.ui.viewmodel.EthioGptViewModel
import kotlinx.coroutines.launch

private data class StarterPrompt(
    val title: String,
    val subtitle: String,
    val prompt: String,
    val toolMode: AiToolMode,
    val language: EthioLanguage
)

private val chatStarterPrompts = listOf(
    StarterPrompt(
        title = "Translate to አማርኛ & Oromo",
        subtitle = "Multilingual translation",
        prompt = "Translate 'Knowledge is power and unity builds the nation' into Amharic, Afaan Oromo, and Tigrinya.",
        toolMode = AiToolMode.TRANSLATOR,
        language = EthioLanguage.ENGLISH
    ),
    StarterPrompt(
        title = "የንግድ ዕቅድ (Business Plan)",
        subtitle = "Amharic business guide",
        prompt = "በአዲስ አበባ አነስተኛ የቴክኖሎጂ ሥራ ለመጀመር ቀላል የንግድ ዕቅድ (Business Plan) በአማርኛ አዘጋጅልኝ።",
        toolMode = AiToolMode.BUSINESS,
        language = EthioLanguage.AMHARIC
    ),
    StarterPrompt(
        title = "Afaan Oromo Study Help",
        subtitle = "Science & homework tutor",
        prompt = "Photosynthesis maali? Barattootaaf haala salphaa ta'een Afaan Oromootiin ibsi.",
        toolMode = AiToolMode.TUTOR,
        language = EthioLanguage.OROMO
    ),
    StarterPrompt(
        title = "Ethiopian Calendar Code",
        subtitle = "Kotlin / Python helper",
        prompt = "Write a clean Python function to convert a Gregorian date into the 13-month Ethiopian calendar.",
        toolMode = AiToolMode.CODER,
        language = EthioLanguage.ENGLISH
    )
)

@Composable
fun ChatScreen(
    viewModel: EthioGptViewModel,
    aiService: EthioAiService,
    voiceManager: VoiceAssistantManager,
    voiceAutoSpeak: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val pendingUserMessage by viewModel.pendingUserMessage.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingReplyText.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedToolMode by viewModel.selectedToolMode.collectAsStateWithLifecycle()
    val composerText by viewModel.composerText.collectAsStateWithLifecycle()
    val attachedBitmap by viewModel.pendingImageBitmap.collectAsStateWithLifecycle()

    val speakingMessageId by voiceManager.speakingMessageId.collectAsStateWithLifecycle()
    val isListening by voiceManager.isListening.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = aiService.loadScaledBitmap(context, uri)
                if (bmp != null) {
                    viewModel.attachImage(uri, bmp)
                } else {
                    Toast.makeText(context, "Could not load selected image.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val startVoiceCapture = {
        voiceManager.startListening(
            language = selectedLanguage,
            onPartialResult = { partial -> viewModel.updateComposerText(partial) },
            onFinalResult = { finalText -> viewModel.updateComposerText(finalText) },
            onError = { errMsg ->
                Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceCapture()
        } else {
            Toast.makeText(context, "Microphone permission is needed for voice input.", Toast.LENGTH_SHORT).show()
        }
    }

    val displayMessages = buildList {
        addAll(messages)
        if (pendingUserMessage != null) add(pendingUserMessage!!)
    }

    LaunchedEffect(displayMessages.size, streamingReply) {
        val totalItems = displayMessages.size + (if (streamingReply != null) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    val triggerSend: () -> Unit = {
        voiceManager.stopListening()
        viewModel.sendMessage { msgId, replyText, lang ->
            if (voiceAutoSpeak) {
                voiceManager.speak(msgId, replyText, lang)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 880.dp)
                .fillMaxSize()
        ) {
            // Top Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            EthioLogoBadge(size = 38.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "EthioGPT",
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Your Ethiopian AI Assistant",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ToolModePill(toolMode = selectedToolMode)
                            IconButton(
                                onClick = {
                                    voiceManager.stopSpeaking()
                                    viewModel.startNewChat()
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("new_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddComment,
                                    contentDescription = "New Chat",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Language Quick Selector
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(EthioLanguage.entries) { lang ->
                            FilterChip(
                                selected = selectedLanguage == lang,
                                onClick = { viewModel.setLanguage(lang) },
                                label = {
                                    Text(
                                        text = lang.nativeName,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = if (lang == EthioLanguage.AMHARIC || lang == EthioLanguage.TIGRINYA) {
                                                NotoEthiopicFontFamily
                                            } else {
                                                MaterialTheme.typography.labelMedium.fontFamily
                                            }
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("lang_chip_${lang.code}")
                            )
                        }
                    }
                    EthioTriColorStripe(height = 2.dp)
                }
            }

            // Messages or Empty Welcome
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (displayMessages.isEmpty() && streamingReply == null) {
                    EmptyChatWelcome(
                        selectedLanguage = selectedLanguage,
                        selectedToolMode = selectedToolMode,
                        onSelectStarter = { starter ->
                            viewModel.setToolMode(starter.toolMode)
                            viewModel.setLanguage(starter.language)
                            viewModel.updateComposerText(starter.prompt)
                        },
                        onPickImage = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = displayMessages,
                            key = { msg -> msg.id.ifBlank { "${msg.role}_${msg.text.hashCode()}" } }
                        ) { msg ->
                            ChatMessageBubble(
                                message = msg,
                                isSpeaking = speakingMessageId == msg.id,
                                onSpeakToggle = {
                                    if (speakingMessageId == msg.id) {
                                        voiceManager.stopSpeaking()
                                    } else {
                                        voiceManager.speak(msg.id, msg.text, selectedLanguage)
                                    }
                                },
                                onCopy = {
                                    copyToClipboard(context, msg.text)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        if (streamingReply != null) {
                            item(key = "streaming_reply") {
                                StreamingReplyBubble(
                                    text = streamingReply!!,
                                    isGenerating = isGenerating
                                )
                            }
                        }
                    }
                }
            }

            // Error Banner
            AnimatedVisibility(visible = chatError != null, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chatError.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearChatError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // Attached Image Preview
            AnimatedVisibility(visible = attachedBitmap != null) {
                attachedBitmap?.let { bmp ->
                    ImageAttachmentBar(
                        bitmap = bmp,
                        onRemove = { viewModel.clearAttachedImage() }
                    )
                }
            }

            // Voice Listening Indicator
            AnimatedVisibility(visible = isListening) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Listening in ${selectedLanguage.displayName}...",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { voiceManager.stopListening() }) {
                            Text("Stop")
                        }
                    }
                }
            }

            // Composer Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("attach_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Attach photo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isListening) {
                                voiceManager.stopListening()
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    startVoiceCapture()
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("voice_input_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (isListening) "Stop voice input" else "Voice input",
                            tint = if (isListening) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = composerText,
                        onValueChange = { viewModel.updateComposerText(it) },
                        placeholder = {
                            Text(
                                text = "Ask in ${selectedLanguage.nativeName}...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { triggerSend() }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_composer_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    val canSend = !isGenerating && (composerText.isNotBlank() || attachedBitmap != null)
                    Surface(
                        shape = CircleShape,
                        color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(48.dp)
                    ) {
                        IconButton(
                            onClick = triggerSend,
                            enabled = canSend,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("send_message_button")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send message",
                                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatWelcome(
    selectedLanguage: EthioLanguage,
    selectedToolMode: AiToolMode,
    onSelectStarter: (StarterPrompt) -> Unit,
    onPickImage: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            EthioLogoBadge(size = 68.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = selectedLanguage.greeting,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = if (selectedLanguage == EthioLanguage.AMHARIC || selectedLanguage == EthioLanguage.TIGRINYA) {
                        NotoEthiopicFontFamily
                    } else {
                        MaterialTheme.typography.headlineSmall.fontFamily
                    }
                ),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${selectedToolMode.title} • ${selectedToolMode.subtitle}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            AssistChip(
                onClick = onPickImage,
                label = { Text("Upload photo or textbook page") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.testTag("empty_state_upload_photo_chip")
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(chatStarterPrompts, key = { it.title }) { starter ->
            Card(
                onClick = { onSelectStarter(starter) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("starter_prompt_${starter.toolMode.code}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = starter.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NotoEthiopicFontFamily
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        ToolModePill(toolMode = starter.toolMode)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = starter.prompt,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = NotoEthiopicFontFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    onSpeakToggle: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            EthioLogoBadge(
                size = 30.dp,
                modifier = Modifier.padding(top = 4.dp, end = 8.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 20.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            tonalElevation = if (isUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 620.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (message.hasImage && isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "Attached image",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Photo attached",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }

                FormattedAiText(
                    text = message.text,
                    isUserMessage = isUser
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = onSpeakToggle,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isSpeaking) "Stop speaking" else "Read aloud",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy message",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingReplyBubble(
    text: String,
    isGenerating: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        EthioLogoBadge(
            size = 30.dp,
            modifier = Modifier.padding(top = 4.dp, end = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 6.dp,
                bottomEnd = 20.dp
            ),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 620.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                if (text.isBlank() && isGenerating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "EthioGPT is thinking...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    FormattedAiText(text = text, isUserMessage = false)
                }
            }
        }
    }
}

@Composable
private fun ImageAttachmentBar(
    bitmap: Bitmap,
    onRemove: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Attached photo preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Image attached",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "Ask a question about this photo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove attached image"
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("EthioGPT Reply", text))
}
