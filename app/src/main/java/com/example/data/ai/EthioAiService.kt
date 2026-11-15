package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.model.AiToolMode
import com.example.data.model.ChatMessage
import com.example.data.model.EthioLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class EthioAiService {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(ETHIOGPT_SYSTEM_INSTRUCTION)
            }
        )
    }

    fun streamAssistantReply(
        historyMessages: List<ChatMessage>,
        userPrompt: String,
        attachedImage: Bitmap?,
        toolMode: AiToolMode,
        language: EthioLanguage
    ): Flow<String> = flow {
        val sdkHistory: List<Content> = historyMessages
            .takeLast(16)
            .map { msg ->
                val role = if (msg.role == "model") "model" else "user"
                content(role) {
                    text(msg.text)
                }
            }

        val chat = model.startChat(history = sdkHistory)

        val directivePrefix = buildString {
            append("[Context: Tool=${toolMode.title}; PreferredOutputLanguage=${language.displayName} (${language.nativeName}). ")
            append(toolMode.systemHint)
            if (language != EthioLanguage.ENGLISH) {
                append(" Respond naturally in ${language.displayName} (${language.nativeName}) unless the user explicitly asks for another language or side-by-side translation.")
            }
            append("]\n\n")
        }

        val promptContent = content {
            if (attachedImage != null) {
                image(attachedImage)
            }
            val effectiveText = if (userPrompt.isBlank() && attachedImage != null) {
                "Please analyze this image and explain what you see clearly."
            } else {
                userPrompt
            }
            text(directivePrefix + effectiveText)
        }

        val accumulated = StringBuilder()
        chat.sendMessageStream(promptContent).collect { chunk ->
            val chunkText = chunk.text.orEmpty()
            if (chunkText.isNotEmpty()) {
                accumulated.append(chunkText)
                emit(accumulated.toString())
            }
        }
    }

    suspend fun loadScaledBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }

            val maxDim = 1280
            var sampleSize = 1
            while (boundsOpts.outWidth / sampleSize > maxDim || boundsOpts.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext null

            val baos = ByteArrayOutputStream()
            decoded.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val bytes = baos.toByteArray()
            if (bytes.size > 15 * 1024 * 1024) {
                Log.w(TAG, "Compressed image exceeded 15MB (${bytes.size} bytes)")
                return@withContext null
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: decoded
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap from URI: $uri", e)
            null
        }
    }

    private companion object {
        const val TAG = "EthioAiService"

        const val ETHIOGPT_SYSTEM_INSTRUCTION = """
You are EthioGPT, an intelligent, warm, and deeply knowledgeable AI assistant built especially for Ethiopian users (students, professionals, entrepreneurs, developers, and everyday citizens).

Core Capabilities & Cultural Fluency:
1. Multilingual Mastery: You communicate naturally, accurately, and idiomatically in Amharic (አማርኛ in Ge'ez script), Afaan Oromo (Qubee orthography), Tigrinya (ትግርኛ in Ge'ez script), Somali (Af-Soomaali), and English. When translating between Ethiopian languages and English, preserve nuance, politeness markers, and clarity.
2. Ethiopian Context & History: You understand Ethiopian history, regions, UNESCO heritage sites (Axum, Lalibela, Gondar, Harar Jugol, Simien Mountains, Tiya, Konso), the 13-month Ethiopian calendar (Ge'ez / Pagume), Ethiopian holidays (Enkutatash, Meskel, Genna, Timkat, Eid, Irreecha), Ethiopian cuisine, coffee culture, and diverse traditions.
3. Education & Homework Help: You support Ethiopian primary, secondary (Grade 1-12, ESSLCE), and university students across Mathematics, Physics, Chemistry, Biology, Civics, History, English, and Amharic with clear step-by-step explanations.
4. Business & Professional Writing: You craft practical business plans (understanding Ethiopian Birr / ETB budgeting, local markets, agriculture, coffee, tech, and retail), formal Amharic and English letters, job applications, CVs, and proposals.
5. Software Engineering: You generate clean, well-commented code in Kotlin, Python, JavaScript, Java, SQL, and more.
6. Multimodal Vision: When given an image, read any printed or handwritten text (including Amharic/Ge'ez or English), analyze diagrams or homework problems, and give clear answers.

Always be accurate, honest about uncertainty, respectful, and helpful.
"""
    }
}
