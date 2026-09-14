package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
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
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.core.protocol.EqBand
import com.alan.ximiearbuds.core.protocol.EqPreset
import com.alan.ximiearbuds.core.protocol.EqualizerState
import com.alan.ximiearbuds.core.protocol.SpatialAudioScene
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 Faithful Reproduction of official SoundEffectActivity (device_settings_activity_soundeffect.xml)
 */
@Composable
fun XiaomiSoundEffectDialog(
    controller: EarbudsController,
    activeModel: EarbudsModel?,
    onDismiss: () -> Unit
) {
    val equalizer by controller.equalizer.collectAsState()
    var showSpatialAudioSubDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(580.dp)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = XiaomiPageBg,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = XiaomiOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringRes("device_settings_sound_settings"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = XiaomiTextPrimary
                        )
                    }
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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sound Enhancement Features Card
                    XiaomiCardContainer {
                        // Audibility Adaptation
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_audibility_adaptation"),
                            subtitle = stringRes("device_settings_audibility_adaptation_desc"),
                            checked = equalizer.audibilityAdaptation,
                            onCheckedChange = { controller.setAudibilityAdaptation(it) }
                        )

                        // Virtual Surround
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_sound_virtual_surround"),
                            subtitle = stringRes("device_settings_sound_virtual_surround_desc"),
                            checked = equalizer.virtualSurround,
                            onCheckedChange = { controller.setVirtualSurround(it) }
                        )

                        // Spatial Audio
                        if (activeModel?.hasSpatialAudio == true) {
                            XiaomiActionItem(
                                title = stringRes("device_settings_sound_spatial_audio"),
                                badgeText = if (equalizer.spatialAudioEnabled) stringRes("common_enable") else stringRes("common_disable"),
                                onClick = { showSpatialAudioSubDialog = true }
                            )
                        }

                        // Adaptive Sense
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_adaptive_sense"),
                            subtitle = stringRes("device_settings_adaptive_sense_detail"),
                            checked = equalizer.adaptiveSense,
                            onCheckedChange = { controller.setAdaptiveSense(it) }
                        )

                        // Adaptive Volume
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_sound_adaptive_volume"),
                            subtitle = stringRes("device_settings_sound_adaptive_volume_desc"),
                            checked = equalizer.adaptiveVolume,
                            onCheckedChange = { controller.setAdaptiveVolume(it) }
                        )

                        // Notification Volume
                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            XiaomiSwitchItem(
                                title = stringRes("device_settings_notification_volume_title"),
                                subtitle = stringRes("device_settings_notification_volume_subtitle"),
                                checked = equalizer.notificationVolumeEnabled,
                                onCheckedChange = { controller.setNotificationVolumeEnabled(it) }
                            )
                            if (equalizer.notificationVolumeEnabled) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Slider(
                                        value = equalizer.notificationVolume.toFloat(),
                                        onValueChange = { controller.setNotificationVolume(it.toInt()) },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = XiaomiOrange,
                                            activeTrackColor = XiaomiOrange
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "${equalizer.notificationVolume}%",
                                        fontSize = 13.sp,
                                        color = XiaomiTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // 14 Official Presets
                    Text(
                        text = stringRes("device_settings_sound_effect_title"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = XiaomiTextPrimary,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    XiaomiCardContainer {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val presets = remember { EqPreset.entries.toList() }
                            // 2-Column Grid of Presets
                            for (row in presets.chunked(2)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (p in row) {
                                        val isSelected = equalizer.preset == p
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) XiaomiOrange.copy(alpha = 0.15f) else Color(0xFF222226))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) XiaomiOrange else Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { controller.setEqPreset(p) }
                                                .padding(horizontal = 10.dp, vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = stringRes(p.stringKey),
                                                color = if (isSelected) XiaomiOrange else XiaomiTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 10-Band Custom Equalizer
                    if (activeModel?.has10BandEq != false) {
                        Text(
                            text = stringRes("device_settings_audio_equalizer"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = XiaomiTextPrimary,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        XiaomiCardContainer {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (band in equalizer.bands) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (band.gainDb > 0) "+${band.gainDb}" else "${band.gainDb}",
                                                fontSize = 10.sp,
                                                color = if (band.gainDb != 0) XiaomiOrange else XiaomiTextMuted,
                                                fontWeight = if (band.gainDb != 0) FontWeight.Bold else FontWeight.Normal
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Slider track
                                            Slider(
                                                value = band.gainDb.toFloat(),
                                                onValueChange = {
                                                    controller.setEqBandGain(band.frequencyHz, it.toInt())
                                                },
                                                valueRange = -10f..10f,
                                                steps = 19,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .width(140.dp)
                                                    .offset(y = 0.dp),
                                                colors = SliderDefaults.colors(
                                                    thumbColor = XiaomiOrange,
                                                    activeTrackColor = XiaomiOrange,
                                                    inactiveTrackColor = Color(0xFF333338)
                                                )
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            val freqLabel = if (band.frequencyHz >= 1000) "${band.frequencyHz / 1000}k" else "${band.frequencyHz}"
                                            Text(
                                                text = freqLabel,
                                                fontSize = 10.sp,
                                                color = XiaomiTextSecondary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { controller.resetEq() }) {
                                        Text(
                                            text = stringRes("common_reset"),
                                            color = XiaomiCyan,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    // Spatial Audio Settings Sub-dialog
    if (showSpatialAudioSubDialog) {
        Dialog(onDismissRequest = { showSpatialAudioSubDialog = false }) {
            Surface(
                modifier = Modifier
                    .width(480.dp)
                    .clip(RoundedCornerShape(20.dp)),
                color = XiaomiCardBg,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringRes("device_settings_sound_spatial_audio"),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = XiaomiTextPrimary
                        )
                        IconButton(
                            onClick = { showSpatialAudioSubDialog = false },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = XiaomiTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    XiaomiSwitchItem(
                        title = stringRes("device_settings_sound_spatial_audio_open"),
                        subtitle = stringRes("device_settings_sound_spatial_audio_desc"),
                        checked = equalizer.spatialAudioEnabled,
                        onCheckedChange = { controller.setSpatialAudio(it) }
                    )

                    if (equalizer.spatialAudioEnabled) {
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_sound_spatial_audio_head_tracking"),
                            subtitle = stringRes("device_settings_sound_spatial_audio_head_tracking_desc"),
                            checked = equalizer.spatialAudioHeadTracking,
                            onCheckedChange = { controller.setSpatialAudioHeadTracking(it) }
                        )

                        Text(
                            text = stringRes("device_settings_sound_spatial_audio_scene_rendering"),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = XiaomiTextSecondary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (scene in SpatialAudioScene.entries) {
                                val isSelected = equalizer.spatialAudioScene == scene
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) XiaomiOrange.copy(alpha = 0.15f) else Color(0xFF222226))
                                        .clickable { controller.setSpatialAudioScene(scene) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringRes(scene.stringKey),
                                        color = if (isSelected) XiaomiOrange else XiaomiTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(XiaomiOrange)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showSpatialAudioSubDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = XiaomiOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringRes("common_confirm"), color = Color.White)
                    }
                }
            }
        }
    }
}
