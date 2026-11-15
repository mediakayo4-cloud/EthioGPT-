package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class EthioLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val ttsLanguageTag: String,
    val speechLocaleTag: String,
    val greeting: String
) {
    ENGLISH("en", "English", "English", "en-US", "en-US", "Selam! How can I assist you today?"),
    AMHARIC("am", "Amharic", "አማርኛ", "am-ET", "am-ET", "ሰላም! ዛሬ በምን ልርዳዎ?"),
    OROMO("om", "Afaan Oromo", "Afaan Oromoo", "om-ET", "en-US", "Akkam! Har'a maaliin si gargaaru?"),
    TIGRINYA("ti", "Tigrinya", "ትግርኛ", "ti-ET", "en-US", "ሰላም! ሎሚ ብምንታይ ክሕግዘካ?"),
    SOMALI("so", "Somali", "Af-Soomaali", "so-SO", "en-US", "Salaam! Sideen ku caawin karaa maanta?");

    companion object {
        fun fromCode(code: String?): EthioLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

enum class ThemeMode(val code: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromCode(code: String?): ThemeMode =
            entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

enum class AiToolMode(
    val code: String,
    val title: String,
    val shortLabel: String,
    val subtitle: String,
    val starterPrompt: String,
    val systemHint: String
) {
    GENERAL(
        code = "general",
        title = "General Assistant",
        shortLabel = "Chat",
        subtitle = "Intelligent answers in Amharic, Afaan Oromo, Tigrinya, and English",
        starterPrompt = "Tell me about Ethiopia's coffee ceremony and its cultural significance.",
        systemHint = "Provide thoughtful, accurate, well-structured answers tailored to Ethiopian users."
    ),
    TRANSLATOR(
        code = "translator",
        title = "Ethiopian Translator",
        shortLabel = "Translate",
        subtitle = "Translate naturally between Amharic, Afaan Oromo, Tigrinya, Somali, and English",
        starterPrompt = "Translate 'Education is the key to building our future' into Amharic, Afaan Oromo, and Tigrinya.",
        systemHint = "Act as an expert Ethiopian linguist and translator. Preserve idioms, tone, Ge'ez script accuracy for Amharic and Tigrinya, and Qubee orthography for Afaan Oromo. Provide clear translations with pronunciation notes when helpful."
    ),
    TUTOR(
        code = "tutor",
        title = "Student & Homework Tutor",
        shortLabel = "Study",
        subtitle = "Step-by-step help for Ethiopian Grade 1-12, ESSLCE, and university courses",
        starterPrompt = "Explain quadratic equations step by step with an example problem for Grade 10.",
        systemHint = "Act as a patient, encouraging Ethiopian educator. Explain concepts step by step, align with Ethiopian secondary and university curricula where relevant, and ask helpful check-in questions."
    ),
    BUSINESS(
        code = "business",
        title = "Business & Formal Letters",
        shortLabel = "Business",
        subtitle = "Business plans in ETB, proposals, CVs, and official Amharic/English letters",
        starterPrompt = "Draft a startup business plan outline for a specialty coffee roastery in Addis Ababa with costs in ETB.",
        systemHint = "Act as an Ethiopian business advisor and executive writer. Understand local Ethiopian market realities, ETB budgeting, trade licensing context, and formal Ethiopian correspondence etiquette."
    ),
    CODER(
        code = "coder",
        title = "Code & Tech Studio",
        shortLabel = "Code",
        subtitle = "Generate, debug, and explain Kotlin, Python, web, and database code",
        starterPrompt = "Write a clean Python function to convert Gregorian dates to the Ethiopian calendar.",
        systemHint = "Act as a senior software engineer. Provide clean, production-ready code with clear fenced code blocks, concise explanations, and edge-case handling."
    ),
    CULTURE(
        code = "culture",
        title = "Culture & Heritage Guide",
        shortLabel = "Heritage",
        subtitle = "Explore Axum, Lalibela, Harar, Gadaa, Ethiopian calendar, and cuisine",
        starterPrompt = "Explain how the 13-month Ethiopian calendar and Enkutatash work.",
        systemHint = "Act as an Ethiopian historian and cultural scholar. Share accurate, respectful insights into Ethiopia's diverse regions, UNESCO heritage sites, calendars, languages, music, and culinary traditions."
    ),
    VISION(
        code = "vision",
        title = "Image & Document Lens",
        shortLabel = "Vision",
        subtitle = "Upload photos of textbook pages, handwritten notes, signs, or objects",
        starterPrompt = "Attach a photo of your notes, textbook, or document and ask me to explain, summarize, or translate it.",
        systemHint = "Analyze uploaded images carefully, transcribe any visible Amharic/English/Oromo text accurately, and answer the user's question clearly."
    );

    companion object {
        fun fromCode(code: String?): AiToolMode =
            entries.firstOrNull { it.code == code } ?: GENERAL
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val preferredLanguage: String = EthioLanguage.ENGLISH.code,
    val themeMode: String = ThemeMode.SYSTEM.code,
    val voiceAutoSpeak: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "New Conversation",
    val toolMode: String = AiToolMode.GENERAL.code,
    val language: String = EthioLanguage.ENGLISH.code,
    val lastMessagePreview: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val text: String = "",
    val toolMode: String = AiToolMode.GENERAL.code,
    val hasImage: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class PromptTemplate(
    val id: String,
    val title: String,
    val description: String,
    val prompt: String,
    val toolMode: AiToolMode,
    val language: EthioLanguage
)
