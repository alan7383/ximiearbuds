package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.alan.ximiearbuds.ui.components.*
import com.alan.ximiearbuds.ui.theme.stringRes

/**
 * 1:1 replica of Xiaomi Earbuds `device_settings_activity_soundeffect.xml`.
 * 
 * Replaces any desktop dialog with an authentic MIUI activity/screen containing:
 * - Audibility adaptation (device_settings_audibility_adaptation)
 * - Virtual Surround 3D (device_settings_sound_virtual_surround)
 * - Spatial Audio link (device_settings_sound_spatial_audio)
 * - Adaptive Volume (device_settings_sound_adaptive_volume)
 * - Notification Volume slider (notification_volume_seekbar)
 * - Customized EQ presets & 10-band studio graphic EQ (included group)
 */
@Composable
fun MiuixSoundEffectsScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToSpatialAudio: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var virtualSurround by remember { mutableStateOf(false) }
    var adaptiveVolume by remember { mutableStateOf(false) }
    var audibilityAdaptation by remember { mutableStateOf(false) }
    var notificationVolume by remember { mutableStateOf(75f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Navigation Bar
        MiuixTopAppBar(
            title = stringRes("device_settings_sound_settings"),
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Section 1: Spatial & Surround Audio
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

            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                if (onNavigateToSpatialAudio != null) {
                    XiaomiActionItem(
                        title = stringRes("device_settings_sound_spatial_audio"),
                        subtitle = stringRes("device_settings_spatial_audio_effect"),
                        onClick = onNavigateToSpatialAudio
                    )
                    XiaomiItemDivider()
                }

                XiaomiSwitchItem(
                    title = stringRes("device_settings_sound_virtual_surround"),
                    subtitle = stringRes("device_settings_sound_virtual_surround_desc"),
                    checked = virtualSurround,
                    onCheckedChange = { virtualSurround = it }
                )

                XiaomiItemDivider()

                XiaomiSwitchItem(
                    title = stringRes("device_settings_sound_adaptive_volume"),
                    subtitle = stringRes("device_settings_sound_adaptive_volume_desc"),
                    checked = adaptiveVolume,
                    onCheckedChange = { adaptiveVolume = it }
                )

                XiaomiItemDivider()

                XiaomiSwitchItem(
                    title = stringRes("device_settings_sound_personalized"),
                    subtitle = stringRes("device_settings_personal_audio_desc"),
                    checked = audibilityAdaptation,
                    onCheckedChange = { audibilityAdaptation = it }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Notification Volume Slider (Replicates NotificationVolumeSeekbar)
            Text(
                text = stringRes("device_settings_notification_volume_title").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringRes("device_settings_notification_volume_subtitle"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${notificationVolume.toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF007AFF)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = notificationVolume,
                        onValueChange = { notificationVolume = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF007AFF),
                            activeTrackColor = Color(0xFF007AFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Equalizer Entry point (Replicates include customized_eq)
            Text(
                text = stringRes("device_settings_audio_equalizer").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                XiaomiActionItem(
                    title = stringRes("device_settings_audio_equalizer"),
                    subtitle = stringRes("device_settings_play_a_song_to_use_this_feature"),
                    onClick = onNavigateToEqualizer
                )
            }
        }
    }
}
