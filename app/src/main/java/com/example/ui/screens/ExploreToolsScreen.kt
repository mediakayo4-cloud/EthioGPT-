package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AiToolMode
import com.example.data.model.EthioLanguage
import com.example.data.model.PromptTemplate
import com.example.ui.components.EthioTriColorStripe
import com.example.ui.theme.NotoEthiopicFontFamily

private val curatedTemplates = listOf(
    PromptTemplate(
        id = "biz_plan",
        title = "Ethiopian Startup Business Plan",
        description = "Generate a complete business plan with startup costs in ETB and Addis/regional market strategy.",
        prompt = "Create a practical business plan for a modern agro-processing & coffee packaging business in Ethiopia, including startup budget in ETB, target customers, and licensing steps.",
        toolMode = AiToolMode.BUSINESS,
        language = EthioLanguage.ENGLISH
    ),
    PromptTemplate(
        id = "am_letter",
        title = "መደበኛ የአማርኛ ደብዳቤ (Formal Letter)",
        description = "Draft polite official Amharic letters for offices, universities, or job applications.",
        prompt = "ለመንግሥት ወይም ለግል ድርጅት የሥራ ማመልከቻ (Application Letter) በተሟላ እና በተከበረ አማርኛ ጻፍልኝ።",
        toolMode = AiToolMode.BUSINESS,
        language = EthioLanguage.AMHARIC
    ),
    PromptTemplate(
        id = "stem_tutor",
        title = "ESSLCE / Grade 12 Study Helper",
        description = "Step-by-step Physics, Chemistry, Math, and Biology explanations with practice questions.",
        prompt = "Give me a clear Grade 12 Physics revision summary on Thermodynamics with 3 ESSLCE-style practice questions and solutions.",
        toolMode = AiToolMode.TUTOR,
        language = EthioLanguage.ENGLISH
    ),
    PromptTemplate(
        id = "oromo_culture",
        title = "Seenaa fi Aadaa Oromoo (Gadaa System)",
        description = "Learn about Ethiopia's UNESCO heritage and Gadaa democratic traditions in Afaan Oromo.",
        prompt = "Sirna Gadaa fi barbaachisummaa isaa seenaa Oromoo fi Itoophiyaa keessatti Afaan Oromootiin bal'inaan ibsi.",
        toolMode = AiToolMode.CULTURE,
        language = EthioLanguage.OROMO
    ),
    PromptTemplate(
        id = "tigrinya_poem",
        title = "ትግርኛ ጽሑፍን ትርጉምን (Tigrinya)",
        description = "Write stories, essays, or translations naturally in Tigrinya.",
        prompt = "ብዛዕባ ኣገዳስነት ትምህርቲ ንመንእሰያት ሓጺር ጽሑፍ ብትግርኛ ጽሓፈለይ።",
        toolMode = AiToolMode.GENERAL,
        language = EthioLanguage.TIGRINYA
    )
)

@Composable
fun ExploreToolsScreen(
    onLaunchTool: (toolMode: AiToolMode, language: EthioLanguage, prefillPrompt: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var sourceLang by remember { mutableStateOf(EthioLanguage.ENGLISH) }
    var targetLang by remember { mutableStateOf(EthioLanguage.AMHARIC) }
    var quickTranslateInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 880.dp)
                .fillMaxSize()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "Explore AI Tools",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Specialized Ethiopian language, education, business, code & vision tools",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            EthioTriColorStripe(height = 2.dp)

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Quick Translator Widget
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Translate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Instant Ethiopian Translator",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("From", style = MaterialTheme.typography.labelSmall)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(EthioLanguage.entries) { lang ->
                                            FilterChip(
                                                selected = sourceLang == lang,
                                                onClick = { sourceLang = lang },
                                                label = { Text(lang.nativeName, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val tmp = sourceLang
                                        sourceLang = targetLang
                                        targetLang = tmp
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SwapHoriz,
                                        contentDescription = "Swap languages"
                                    )
                                }
                            }

                            Text("To", style = MaterialTheme.typography.labelSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(EthioLanguage.entries) { lang ->
                                    FilterChip(
                                        selected = targetLang == lang,
                                        onClick = { targetLang = lang },
                                        label = { Text(lang.nativeName, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = quickTranslateInput,
                                onValueChange = { quickTranslateInput = it },
                                placeholder = { Text("Enter text to translate to ${targetLang.nativeName}...") },
                                maxLines = 3,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quick_translate_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val textToTranslate = quickTranslateInput.trim()
                                        .ifEmpty { "Hello! How are you doing today?" }
                                    val prompt = "Translate the following text naturally from ${sourceLang.displayName} into ${targetLang.displayName} (${targetLang.nativeName}), and briefly explain any key cultural nuances:\n\n\"$textToTranslate\""
                                    onLaunchTool(AiToolMode.TRANSLATOR, targetLang, prompt)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quick_translate_button")
                            ) {
                                Text("Translate with EthioGPT")
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "AI Workspaces",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(AiToolMode.entries, key = { it.code }) { mode ->
                    Card(
                        onClick = { onLaunchTool(mode, EthioLanguage.ENGLISH, mode.starterPrompt) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("explore_tool_${mode.code}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = toolIcon(mode),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = mode.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open ${mode.title}",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ready-to-Use Ethiopian Templates",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(curatedTemplates, key = { it.id }) { tpl ->
                    Card(
                        onClick = { onLaunchTool(tpl.toolMode, tpl.language, tpl.prompt) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("template_${tpl.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = tpl.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = NotoEthiopicFontFamily
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tpl.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun toolIcon(mode: AiToolMode): ImageVector = when (mode) {
    AiToolMode.GENERAL -> Icons.Filled.AutoAwesome
    AiToolMode.TRANSLATOR -> Icons.Filled.Translate
    AiToolMode.TUTOR -> Icons.Filled.School
    AiToolMode.BUSINESS -> Icons.Filled.BusinessCenter
    AiToolMode.CODER -> Icons.Filled.Code
    AiToolMode.CULTURE -> Icons.Filled.Museum
    AiToolMode.VISION -> Icons.Filled.ImageSearch
}
