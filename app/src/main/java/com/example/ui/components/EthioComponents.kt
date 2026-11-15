package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AiToolMode
import com.example.ui.theme.EthioCrimsonTertiary
import com.example.ui.theme.EthioGreenPrimary
import com.example.ui.theme.EthioSunYellow
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NotoEthiopicFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.containsEthiopicScript

@Composable
fun EthioTriColorStripe(
    modifier: Modifier = Modifier,
    height: Dp = 3.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        EthioGreenPrimary,
                        EthioSunYellow,
                        EthioCrimsonTertiary
                    )
                )
            )
    )
}

@Composable
fun EthioLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Image(
        painter = painterResource(id = R.drawable.img_ai_logo_mark),
        contentDescription = "EthioGPT logo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(EthioGreenPrimary, EthioSunYellow, EthioCrimsonTertiary)
                ),
                shape = CircleShape
            )
    )
}

@Composable
fun FormattedAiText(
    text: String,
    modifier: Modifier = Modifier,
    isUserMessage: Boolean = false
) {
    val segments = parseCodeSegments(text)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        segments.forEach { segment ->
            if (segment.isCode) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isUserMessage) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (segment.language.isNotBlank()) {
                            Text(
                                text = segment.language.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = segment.content,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = if (isUserMessage) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            } else {
                val useEthiopicFont = containsEthiopicScript(segment.content)
                Text(
                    text = segment.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = if (useEthiopicFont) NotoEthiopicFontFamily else PlusJakartaFontFamily
                    ),
                    color = if (isUserMessage) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

private data class TextSegment(
    val content: String,
    val isCode: Boolean,
    val language: String = ""
)

private fun parseCodeSegments(raw: String): List<TextSegment> {
    val result = mutableListOf<TextSegment>()
    val regex = Regex("```([a-zA-Z0-9_+-]*)\\n?([\\s\\S]*?)```")
    var lastIndex = 0
    for (match in regex.findAll(raw)) {
        if (match.range.first > lastIndex) {
            val plain = raw.substring(lastIndex, match.range.first).trim()
            if (plain.isNotEmpty()) {
                result.add(TextSegment(plain, isCode = false))
            }
        }
        val lang = match.groupValues[1].trim()
        val code = match.groupValues[2].trimEnd()
        if (code.isNotEmpty()) {
            result.add(TextSegment(code, isCode = true, language = lang))
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < raw.length) {
        val tail = raw.substring(lastIndex).trim()
        if (tail.isNotEmpty()) {
            result.add(TextSegment(tail, isCode = false))
        }
    }
    if (result.isEmpty() && raw.isNotEmpty()) {
        result.add(TextSegment(raw, isCode = false))
    }
    return result
}

@Composable
fun ToolModePill(
    toolMode: AiToolMode,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = toolMode.shortLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
