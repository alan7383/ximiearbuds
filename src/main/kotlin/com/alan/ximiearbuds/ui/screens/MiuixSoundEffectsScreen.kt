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
import com.alan.ximiearbuds.core.device.DeviceRegistry
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.core.protocol.OfficialFunctionIds
import com.alan.ximiearbuds.ui.components.*
import com.alan.ximiearbuds.ui.theme.stringRes

/**
 * 1:1 replica of Xiaomi Earbuds `device_settings_activity_soundeffect.xml`.
 * 
 * Adapts dynamically to each earbud model's soundCapabilities and official function IDs:
 * - Spatial Audio link (FUNC_SOUND_SETTING_CLOSE_SPATIAL_AUDIO / 2002)
 * - Virtual Surround 3D (FUNC_SOUND_SETTING_VIRTUAL_SURROUND / 2001)
 * - Adaptive Sense (FUNC_SOUND_SETTINGS_ADAPTIVE_SENSE / 2008)
 * - Adaptive Volume (FUNC_SOUND_SETTINGS_ADAPTIVE_VOLUME / 2017)
 * - Audibility Adaptation (FUNC_AUDIBILITY_ADAPTATION / 2019)
 * - Notification Volume slider (FUNC_NOTIFICATION_VOLUME / 2021)
 * - Customized EQ presets & 10-band studio graphic EQ (FUNC_SOUND_SETTINGS_SOUND_MODE / 2006, 2016)
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
    val activeModel by controller.activeModel.collectAsState()
    val model = activeModel ?: DeviceRegistry.GENERIC_MODEL
    val soundCaps = model.soundCapabilities
    val eqState by controller.equalizer.collectAsState()
    val quickSettings by controller.quickSettings.collectAsState()

    // Capability checks matching decompiled SoundEffectActivity.java
    val hasFunctions = model.supportedFunctionIds.isNotEmpty()
    val showSpatialAudio = (if (hasFunctions) model.hasFunction(OfficialFunctionIds.FUNC_SOUND_SETTING_CLOSE_SPATIAL_AUDIO) || soundCaps.hasSpatialAudio else true) && onNavigateToSpatialAudio != null
    val showVirtualSurround = if (hasFunctions) model.hasFunction(OfficialFunctionIds.FUNC_SOUND_SETTING_VIRTUAL_SURROUND) || soundCaps.hasVirtualSurround else true
    val showAdaptiveSense = if (hasFunctions) model.hasFunction(OfficialFunctionIds.FUNC_SOUND_SETTINGS_ADAPTIVE_SENSE) || soundCaps.hasAdaptiveSense else false
    val showAdaptiveVolume = if (hasFunctions) model.hasFunction(OfficialFunctionIds.FUNC_SOUND_SETTINGS_ADAPTIVE_VOLUME) || soundCaps.hasAdaptiveVolume else true
    val showAudibilityAdaptation = if (hasFunctions) model.hasFunction(OfficialFunctionIds.FUNC_AUDIBILITY_ADAPTATION) || soundCaps.hasAudibilityAdaptation else true
    val showNotificationVolume = if (hasFunctions) model.hasFunction(OfficialFunctionIds.FUNC_NOTIFICATION_VOLUME) || soundCaps.hasNotificationVolume else true
    val showEqualizer = if (hasFunctions) (model.hasFunction(OfficialFunctionIds.FUNC_SOUND_SETTINGS_SOUND_MODE) || model.hasFunction(OfficialFunctionIds.FUNC_SOUND_SETTINGS_SOUND_MODE_OTA) || soundCaps.supportedPresets.isNotEmpty() || model.has10BandEq) else true

    val hasSoundSettingItems = showSpatialAudio || showVirtualSurround || showAdaptiveSense || showAdaptiveVolume || showAudibilityAdaptation

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

            // Section 1: Spatial & Surround Audio Effects Card
            if (hasSoundSettingItems) {
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
                    var needsDivider = false

                    if (showSpatialAudio) {
                        XiaomiActionItem(
                            title = stringRes("device_settings_sound_spatial_audio"),
                            subtitle = stringRes("device_settings_spatial_audio_effect"),
                            onClick = onNavigateToSpatialAudio
                        )
                        needsDivider = true
                    }

                    if (showVirtualSurround) {
                        if (needsDivider) XiaomiItemDivider()
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_sound_virtual_surround"),
                            subtitle = stringRes("device_settings_sound_virtual_surround_desc"),
                            checked = eqState.virtualSurround,
                            onCheckedChange = { controller.setVirtualSurround(it) }
                        )
                        needsDivider = true
                    }

                    if (showAdaptiveSense) {
                        if (needsDivider) XiaomiItemDivider()
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_adaptive_sense"),
                            subtitle = stringRes("device_settings_adaptive_sense_detail"),
                            checked = eqState.adaptiveSense,
                            onCheckedChange = { controller.setAdaptiveSense(it) }
                        )
                        needsDivider = true
                    }

                    if (showAdaptiveVolume) {
                        if (needsDivider) XiaomiItemDivider()
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_sound_adaptive_volume"),
                            subtitle = stringRes("device_settings_sound_adaptive_volume_desc"),
                            checked = quickSettings.adaptiveVolume,
                            onCheckedChange = { controller.setAdaptiveVolume(it) }
                        )
                        needsDivider = true
                    }

                    if (showAudibilityAdaptation) {
                        if (needsDivider) XiaomiItemDivider()
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_sound_personalized"),
                            subtitle = stringRes("device_settings_personal_audio_desc"),
                            checked = eqState.audibilityAdaptation,
                            onCheckedChange = { controller.setAudibilityAdaptation(it) }
                        )
                    }
                }
            }

            // Section 2: Notification Volume Slider (Replicates NotificationVolumeSeekbar)
            if (showNotificationVolume) {
                Spacer(modifier = Modifier.height(20.dp))

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
                                text = "${eqState.notificationVolume}%",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF007AFF)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = eqState.notificationVolume.toFloat(),
                            onValueChange = { controller.setNotificationVolume(it.toInt()) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF007AFF),
                                activeTrackColor = Color(0xFF007AFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Section 3: Equalizer Entry point (Replicates include customized_eq)
            if (showEqualizer) {
                Spacer(modifier = Modifier.height(20.dp))

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
}
