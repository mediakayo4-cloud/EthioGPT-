package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AiToolMode
import com.example.data.model.EthioLanguage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class EthioGptRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_and_updateUserPreferences_succeedsForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = EthioGptRepository(firestore, auth)

        val ensureResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile(displayName = "Alice Tadesse", email = ALICE_EMAIL)
        }
        assertTrue(ensureResult.isSuccess)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateUserPreferences(
                preferredLanguage = EthioLanguage.AMHARIC.code,
                themeMode = "dark",
                voiceAutoSpeak = true
            )
        }
        assertTrue(updateResult.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it != null }
        }
        assertEquals("Alice Tadesse", profile?.displayName)
        assertEquals("am", profile?.preferredLanguage)
        assertEquals("dark", profile?.themeMode)
        assertEquals(true, profile?.voiceAutoSpeak)
    }

    @Test
    fun saveExchange_and_observeConversations_persistsAndStreamsMessages() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = EthioGptRepository(firestore, auth)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveExchange(
                conversationId = null,
                title = "Ethiopian History",
                toolMode = AiToolMode.CULTURE.code,
                language = EthioLanguage.ENGLISH.code,
                userText = "Tell me about Lalibela.",
                modelText = "Lalibela is famous for its monolithic rock-hewn churches.",
                hasImage = false
            )
        }
        assertTrue(saveResult.isSuccess)
        val conversationId = saveResult.getOrThrow()
        assertTrue(conversationId.isNotBlank())

        val conversations = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeConversations(uid).first { list -> list.any { it.id == conversationId } }
        }
        assertTrue(conversations.any { it.id == conversationId && it.messageCount == 2L })

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMessages(uid, conversationId).first { list -> list.size == 2 }
        }
        assertEquals(listOf("user", "model"), messages.map { it.role })

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteConversation(conversationId)
        }
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = EthioGptRepository(firestore, auth)
        val convId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveExchange(
                conversationId = null,
                title = "Private Notes",
                toolMode = AiToolMode.GENERAL.code,
                language = EthioLanguage.ENGLISH.code,
                userText = "Secret note",
                modelText = "Understood.",
                hasImage = false
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("conversations").document(convId)
                    .get().await()
            }
            fail("Expected PERMISSION_DENIED for cross-user read")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun observeConversations_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = EthioGptRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeConversations("alice_uid").first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice.ethiogpt@test.com"
        const val BOB_EMAIL = "bob.ethiogpt@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
