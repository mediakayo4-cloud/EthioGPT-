package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.AiToolMode
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.EthioLanguage
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class EthioGptRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path, auth)
                }
                throw error
            }
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String?
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val safeName = displayName.trim().ifEmpty { "EthioGPT User" }.take(80)
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "preferredLanguage" to EthioLanguage.ENGLISH.code,
                    "themeMode" to ThemeMode.SYSTEM.code,
                    "voiceAutoSpeak" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!email.isNullOrBlank()) {
                    payload["email"] = email.trim().take(120)
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateUserPreferences(
        displayName: String? = null,
        preferredLanguage: String? = null,
        themeMode: String? = null,
        voiceAutoSpeak: Boolean? = null
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!displayName.isNullOrBlank()) {
            updates["displayName"] = displayName.trim().take(80)
        }
        if (preferredLanguage != null) {
            updates["preferredLanguage"] = preferredLanguage
        }
        if (themeMode != null) {
            updates["themeMode"] = themeMode
        }
        if (voiceAutoSpeak != null) {
            updates["voiceAutoSpeak"] = voiceAutoSpeak
        }
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> {
        val colRef = db.collection("users").document(userId).collection("conversations")
        return colRef.orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Conversation::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path, auth)
                }
                throw error
            }
    }

    fun observeMessages(userId: String, conversationId: String): Flow<List<ChatMessage>> {
        val colRef = db.collection("users").document(userId)
            .collection("conversations").document(conversationId)
            .collection("messages")
        return colRef.orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path, auth)
                }
                throw error
            }
    }

    suspend fun createConversation(
        title: String,
        toolMode: String = AiToolMode.GENERAL.code,
        language: String = EthioLanguage.ENGLISH.code,
        preview: String = ""
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
            .collection("conversations").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().ifEmpty { "New Chat" }.take(120),
            "toolMode" to toolMode,
            "language" to language,
            "lastMessagePreview" to preview.take(300),
            "messageCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun saveExchange(
        conversationId: String?,
        title: String,
        toolMode: String,
        language: String,
        userText: String,
        modelText: String,
        hasImage: Boolean
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val conversationsCol = db.collection("users").document(uid).collection("conversations")
        val isNewConversation = conversationId.isNullOrBlank()
        val convRef = if (isNewConversation) conversationsCol.document() else conversationsCol.document(conversationId!!)
        val userMsgRef = convRef.collection("messages").document()
        val modelMsgRef = convRef.collection("messages").document()

        val safeUserText = userText.trim().ifEmpty { "[Image attached]" }.take(32000)
        val safeModelText = modelText.trim().ifEmpty { "..." }.take(32000)
        val preview = safeModelText.replace('\n', ' ').take(300)

        return try {
            val batch = db.batch()
            if (isNewConversation) {
                val convPayload = mapOf(
                    "userId" to uid,
                    "title" to title.trim().ifEmpty { "New Chat" }.take(120),
                    "toolMode" to toolMode,
                    "language" to language,
                    "lastMessagePreview" to preview,
                    "messageCount" to 2L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(convRef, convPayload)
            } else {
                val convUpdates = mutableMapOf<String, Any>(
                    "toolMode" to toolMode,
                    "language" to language,
                    "lastMessagePreview" to preview,
                    "messageCount" to FieldValue.increment(2L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.update(convRef, convUpdates)
            }

            batch.set(
                userMsgRef,
                mapOf(
                    "userId" to uid,
                    "role" to "user",
                    "text" to safeUserText,
                    "toolMode" to toolMode,
                    "hasImage" to hasImage,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                modelMsgRef,
                mapOf(
                    "userId" to uid,
                    "role" to "model",
                    "text" to safeModelText,
                    "toolMode" to toolMode,
                    "hasImage" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(convRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, convRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val convRef = db.collection("users").document(uid)
            .collection("conversations").document(conversationId)
        return try {
            val messagesSnapshot = convRef.collection("messages").get().await()
            val docs = messagesSnapshot.documents
            for (chunk in docs.chunked(400)) {
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            convRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, convRef.path, auth)
            Result.failure(e)
        }
    }
}
