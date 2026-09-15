package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.core.protocol.EqBand
import com.alan.ximiearbuds.core.protocol.EqPreset
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.theme.stringRes

/**
 * 1:1 replica of Xiaomi Earbuds `device_settings_fragment_customized_eq.xml`.
 * 
 * Replaces the popup dialog with an authentic full-screen MIUI fragment:
 * - Top navigation bar with back arrow
 * - Preset selector chips (Standard, Vocal, Treble, Bass, Custom)
 * - 10-Band graphic equalizer with vertical gain bars (-10 dB to +10 dB)
 * - Curve reset button with official confirmation dialog
 */
@Composable
fun MiuixEqualizerScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eqState by controller.equalizer.collectAsState()
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringRes("device_settings_audio_equalizer"), fontWeight = FontWeight.Bold) },
            text = { Text(stringRes("device_settings_whether_to_clear_equalizer_params")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        controller.resetEq()
                        showResetDialog = false
                    }
                ) {
                    Text(stringRes("device_settings_fit_detection_dialog_confirm"), color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringRes("device_settings_dialog_cancle"))
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top MIUI Navigation Bar
        MiuixTopAppBar(
            title = stringRes("device_settings_audio_equalizer"),
            onBackClick = onBackClick,
            actions = {
                IconButton(onClick = { showResetDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringRes("device_settings_whether_to_clear_equalizer_params"),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Presets Section Header
            Text(
                text = stringRes("device_settings_sound_settings").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            // Preset Pills Container
            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    val presets = listOf(
                        EqPreset.STANDARD to stringRes("device_settings_sound_balanced"),
                        EqPreset.VOICE to stringRes("device_settings_sound_vocal_enhancement"),
                        EqPreset.TREBLE to stringRes("device_settings_sound_treble_boost"),
                        EqPreset.BASS to stringRes("device_settings_sound_bass_boost"),
                        EqPreset.CUSTOM to stringRes("device_settings_sound_custom")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for ((preset, label) in presets) {
                            val isSelected = eqState.preset == preset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF007AFF) else Color.Transparent)
                                    .clickable { controller.setEqPreset(preset) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 10-Band Graphic Studio Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COURBE DE GAIN (10 BANDES)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )

                Text(
                    text = "-10 dB à +10 dB",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }

            // Sliders Container
            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 12.dp)
                ) {
                    // Sliders Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (band in eqState.bands) {
                            MiuixVerticalEqSlider(
                                band = band,
                                onGainChange = { newGain ->
                                    controller.setEqBandGain(band.frequencyHz, newGain)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixVerticalEqSlider(
    band: EqBand,
    onGainChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Gain dB Indicator
        Text(
            text = if (band.gainDb > 0) "+${band.gainDb}" else "${band.gainDb}",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (band.gainDb != 0) Color(0xFF007AFF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )

        // Vertical Slider Track
        Box(
            modifier = Modifier
                .width(18.dp)
                .weight(1f)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background track line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            )

            Slider(
                value = band.gainDb.toFloat(),
                onValueChange = { onGainChange(it.toInt()) },
                valueRange = -10f..10f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF007AFF),
                    activeTrackColor = Color(0xFF007AFF),
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier
                    .requiredWidth(140.dp)
                    .requiredHeight(24.dp)
                    .rotate(-90f)
            )
        }

        // Frequency Label (31Hz, 1kHz, etc.)
        val freqLabel = when {
            band.frequencyHz >= 1000 -> "${band.frequencyHz / 1000}k"
            else -> "${band.frequencyHz}"
        }

        Text(
            text = freqLabel,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}
