package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.JsonBraceColor
import com.example.ui.theme.JsonKeyColor
import com.example.ui.theme.JsonStringColor
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VeloBackground
import com.example.ui.theme.VeloCardBorder
import com.example.ui.theme.VeloSurface

@Composable
fun JsonResponseCard(
    rawJson: String,
    actionName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VeloSurface)
            .border(1.dp, VeloCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("json_response_card")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Terminal Icon",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Velo JSON Output",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 0.8.sp
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Action Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ElectricViolet.copy(alpha = 0.2f))
                        .border(1.dp, ElectricViolet.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = actionName,
                        color = ElectricViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Copy button
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Velo JSON Response", rawJson)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("copy_json_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy JSON",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Code Box with Syntax Highlighting
        val formattedJson = prettyPrintJson(rawJson)
        val annotatedText = buildAnnotatedJsonString(formattedJson)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(VeloBackground)
                .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Text(
                text = annotatedText,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.testTag("json_text_content")
            )
        }
    }
}

private fun prettyPrintJson(raw: String): String {
    return try {
        org.json.JSONObject(raw).toString(2)
    } catch (_: Exception) {
        raw
    }
}

@Composable
private fun buildAnnotatedJsonString(jsonString: String) = buildAnnotatedString {
    var inQuotes = false
    var isKey = true
    val currentWord = StringBuilder()

    for (char in jsonString) {
        when {
            char == '{' || char == '}' || char == '[' || char == ']' -> {
                withStyle(SpanStyle(color = JsonBraceColor, fontWeight = FontWeight.Bold)) {
                    append(char)
                }
            }
            char == ':' -> {
                isKey = false
                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.6f))) {
                    append(char)
                }
            }
            char == ',' -> {
                isKey = true
                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.6f))) {
                    append(char)
                }
            }
            char == '"' -> {
                inQuotes = !inQuotes
                val color = if (isKey) JsonKeyColor else JsonStringColor
                withStyle(SpanStyle(color = color)) {
                    append(char)
                }
            }
            else -> {
                val color = if (inQuotes) {
                    if (isKey) JsonKeyColor else JsonStringColor
                } else {
                    Color(0xFFE2E8F0)
                }
                withStyle(SpanStyle(color = color)) {
                    append(char)
                }
            }
        }
    }
}
