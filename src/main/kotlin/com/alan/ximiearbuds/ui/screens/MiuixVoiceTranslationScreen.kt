package com.alan.ximiearbuds.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 authentic reproduction of `device_settings_record_list_activity.xml`
 * (com.mi.earphone.settings.ui.voicetranslation.AudioRecordListActivity).
 */
@Composable
fun MiuixVoiceTranslationScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRecording by remember { mutableStateOf(false) }
    var sourceLang by remember { mutableStateOf("Français") }
    var targetLang by remember { mutableStateOf("English") }
    val scrollState = rememberScrollState()

    val pulseTransition = rememberInfiniteTransition(label = "recPulse")
    val pulseSize by pulseTransition.animateFloat(
        initialValue = 72f,
        targetValue = 92f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XiaomiPageBg)
    ) {
        MiuixTopAppBar(
            title = stringRes("device_settings_translate_title"),
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Dictaphone / Waveform card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(XiaomiCardBg),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isRecording) pulseSize.dp else 72.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) XiaomiRed.copy(alpha = 0.2f) else XiaomiCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { isRecording = !isRecording },
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isRecording) XiaomiRed else XiaomiCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isRecording) stringRes("device_settings_translating") else stringRes("device_settings_translate_title"),
                        color = XiaomiTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = stringRes("device_settings_face_to_face_guide_hint"),
                        color = XiaomiTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Language Pair Card
            Text(
                text = stringRes("device_settings_face_to_face_guide_title"),
                color = XiaomiTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
            )

            XiaomiCardContainer {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sourceLang,
                        color = XiaomiCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = XiaomiTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = targetLang,
                        color = XiaomiCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Recent Transcriptions
            Text(
                text = stringRes("device_settings_record_file_title"),
                color = XiaomiTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
            )

            XiaomiCardContainer {
                val samples = listOf(
                    "Réunion projet XimiEarbuds" to ("15 sept. 2026 • 04:12" to "1.2 Mo"),
                    "Note vocale Protocole Bluetooth" to ("14 sept. 2026 • 01:45" to "480 Ko")
                )

                samples.forEachIndexed { index, (title, meta) ->
                    val (date, size) = meta
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = title,
                                color = XiaomiTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$date • $size",
                                color = XiaomiTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (index < samples.size - 1) {
                        XiaomiItemDivider()
                    }
                }
            }
        }
    }
}
