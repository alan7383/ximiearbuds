package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alan.ximiearbuds.core.protocol.EqBand
import com.alan.ximiearbuds.core.protocol.EqPreset
import com.alan.ximiearbuds.core.protocol.EqualizerState
import com.alan.ximiearbuds.ui.theme.*

@Composable
fun XiaomiEqualizerDialog(
    equalizerState: EqualizerState,
    onPresetChange: (EqPreset) -> Unit,
    onBandChange: (frequencyHz: Int, gainDb: Int) -> Unit,
    onResetBands: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(620.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(XiaomiCardBg)
                .border(1.dp, XiaomiCardBorder, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header: Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringRes("device_settings_sound_settings"),
                        color = XiaomiTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = XiaomiTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets Pills Row
                Text(
                    text = stringRes("device_settings_sound_mode"),
                    color = XiaomiTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141416))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val presets = listOf(
                        EqPreset.STANDARD to stringRes("device_settings_sound_balanced"),
                        EqPreset.VOICE to stringRes("device_settings_sound_vocal_enhancement"),
                        EqPreset.TREBLE to stringRes("device_settings_sound_treble_boost"),
                        EqPreset.BASS to stringRes("device_settings_sound_bass_boost"),
                        EqPreset.CUSTOM to stringRes("device_settings_audio_equalizer")
                    )

                    for ((preset, label) in presets) {
                        val isSelected = equalizerState.preset == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) XiaomiCyan else Color.Transparent)
                                .clickable { onPresetChange(preset) }
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else XiaomiTextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Equalizer Sliders Header (with Reset button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringRes("device_settings_audio_equalizer") + " (±10 dB)",
                        color = XiaomiTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Reset button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onResetBands)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = XiaomiTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset",
                            color = XiaomiTextSecondary,
                            fontSize = 12.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 10-Band Graphic Equalizer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141416))
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (band in equalizerState.bands) {
                        BandSliderColumn(
                            band = band,
                            onGainChange = { newGain -> onBandChange(band.frequencyHz, newGain) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BandSliderColumn(
    band: EqBand,
    onGainChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(44.dp).fillMaxHeight()
    ) {
        // Gain value (+3, 0, -2)
        Text(
            text = if (band.gainDb > 0) "+${band.gainDb}" else "${band.gainDb}",
            color = if (band.gainDb != 0) XiaomiCyan else XiaomiTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Vertical Slider Track
        Box(
            modifier = Modifier
                .weight(1f)
                .width(28.dp),
            contentAlignment = Alignment.Center
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Color(0xFF28282C))
            )

            // Center 0dB indicator tick
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(2.dp)
                    .background(Color(0xFF44444A))
            )

            // Interactive slider (-10 to +10)
            Slider(
                value = band.gainDb.toFloat(),
                onValueChange = { onGainChange(it.toInt()) },
                valueRange = -10f..10f,
                steps = 19,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(28.dp),
                colors = SliderDefaults.colors(
                    thumbColor = XiaomiCyan,
                    activeTrackColor = XiaomiCyan,
                    inactiveTrackColor = Color(0xFF28282C)
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Frequency label (e.g. 31, 1k, 16k)
        val freqLabel = when {
            band.frequencyHz >= 1000 -> "${band.frequencyHz / 1000}k"
            else -> "${band.frequencyHz}"
        }

        Text(
            text = freqLabel,
            color = XiaomiTextSecondary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
