package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.alan.ximiearbuds.core.protocol.EarboxSoundState
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiActionItem
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.components.XiaomiSwitchItem
import com.alan.ximiearbuds.ui.theme.XiaomiCyan
import com.alan.ximiearbuds.ui.theme.stringRes
import androidx.compose.ui.res.painterResource

/**
 * 1:1 authentic reproduction of Xiaomi Earbuds `device_settings_fragment_earbox_sound.xml`
 * & `EarBoxSettingFragment.java`.
 *
 * Configured with authentic Xiaomi sound profiles:
 * - Arctic (Carillon)
 * - BB (Scintillement)
 * - Fireflies (Étincelle)
 * - Australia (Disparaître)
 */
@Composable
fun MiuixEarboxSoundScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val earboxSound by controller.earboxSound.collectAsState()
    val scrollState = rememberScrollState()

    var activeDialogType by remember { mutableStateOf<Int?>(null) }

    val openSoundEnabled = earboxSound.openSound.soundId > 0
    val closeSoundEnabled = earboxSound.closeSound.soundId > 0
    val chargeSoundEnabled = earboxSound.chargeSound.soundId > 0
    val currentVolume = earboxSound.chargeSound.volume.toFloat().coerceIn(0f, 100f)

    val soundToneNames = listOf(
        0 to stringRes("device_settings_earbox_no_sound"),
        1 to stringRes("device_settings_earbox_arctic"),
        2 to stringRes("device_settings_earbox_bb"),
        3 to stringRes("device_settings_earbox_fireflies"),
        4 to stringRes("device_settings_earbox_australia")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Navigation Bar
        MiuixTopAppBar(
            title = stringRes("device_settings_earbox_sound"),
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

            // Volume Section Header
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

            // Volume Card with Official Case Sound Asset
            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(
                                    if (currentVolume > 0f) "drawable/device_settings_ear_box_sound.png"
                                    else "drawable/device_settings_ear_box_sound_mute.png"
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringRes("device_settings_earbox_sound"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${currentVolume.toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = XiaomiCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = currentVolume,
                        onValueChange = { newVol ->
                            controller.setEarboxVolume(EarboxSoundState.SOUND_TYPE_OPEN, newVol.toInt())
                            controller.setEarboxVolume(EarboxSoundState.SOUND_TYPE_CLOSE, newVol.toInt())
                            controller.setEarboxVolume(EarboxSoundState.SOUND_TYPE_CHARGE, newVol.toInt())
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = XiaomiCyan,
                            activeTrackColor = XiaomiCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Events Section Header
            Text(
                text = stringRes("device_settings_function_settings").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                // Opening Sound Toggle & Selection
                XiaomiSwitchItem(
                    title = stringRes("device_settings_earbox_opening_sound"),
                    subtitle = soundToneNames.find { it.first == earboxSound.openSound.soundId }?.second ?: stringRes("device_settings_earbox_arctic"),
                    iconRes = "drawable/device_settings_opening_sound.png",
                    checked = openSoundEnabled,
                    onCheckedChange = { enabled ->
                        controller.setEarboxSoundId(EarboxSoundState.SOUND_TYPE_OPEN, if (enabled) 1 else 0)
                    }
                )

                XiaomiItemDivider()

                // Closing Sound Toggle & Selection
                XiaomiSwitchItem(
                    title = stringRes("device_settings_earbox_closing_sound"),
                    subtitle = soundToneNames.find { it.first == earboxSound.closeSound.soundId }?.second ?: stringRes("device_settings_earbox_arctic"),
                    iconRes = "drawable/device_settings_closing_sound.png",
                    checked = closeSoundEnabled,
                    onCheckedChange = { enabled ->
                        controller.setEarboxSoundId(EarboxSoundState.SOUND_TYPE_CLOSE, if (enabled) 1 else 0)
                    }
                )

                XiaomiItemDivider()

                // Charge Sound Toggle & Selection
                XiaomiSwitchItem(
                    title = stringRes("device_settings_earbox_charge_sound"),
                    subtitle = soundToneNames.find { it.first == earboxSound.chargeSound.soundId }?.second ?: stringRes("device_settings_earbox_arctic"),
                    iconRes = "drawable/device_settings_charge_icon.png",
                    checked = chargeSoundEnabled,
                    onCheckedChange = { enabled ->
                        controller.setEarboxSoundId(EarboxSoundState.SOUND_TYPE_CHARGE, if (enabled) 1 else 0)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sound Tone Customization Section
            Text(
                text = stringRes("device_settings_skin_title").uppercase(),
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
                    title = stringRes("device_settings_earbox_opening_sound"),
                    subtitle = soundToneNames.find { it.first == earboxSound.openSound.soundId }?.second ?: stringRes("device_settings_earbox_arctic"),
                    iconRes = "drawable/device_settings_opening_sound.png",
                    onClick = { activeDialogType = EarboxSoundState.SOUND_TYPE_OPEN }
                )

                XiaomiItemDivider()

                XiaomiActionItem(
                    title = stringRes("device_settings_earbox_closing_sound"),
                    subtitle = soundToneNames.find { it.first == earboxSound.closeSound.soundId }?.second ?: stringRes("device_settings_earbox_arctic"),
                    iconRes = "drawable/device_settings_closing_sound.png",
                    onClick = { activeDialogType = EarboxSoundState.SOUND_TYPE_CLOSE }
                )

                XiaomiItemDivider()

                XiaomiActionItem(
                    title = stringRes("device_settings_earbox_charge_sound"),
                    subtitle = soundToneNames.find { it.first == earboxSound.chargeSound.soundId }?.second ?: stringRes("device_settings_earbox_arctic"),
                    iconRes = "drawable/device_settings_charge_icon.png",
                    onClick = { activeDialogType = EarboxSoundState.SOUND_TYPE_CHARGE }
                )
            }
        }
    }

    // Modal Sheet for choosing sound tone
    activeDialogType?.let { soundType ->
        val currentSoundId = when (soundType) {
            EarboxSoundState.SOUND_TYPE_OPEN -> earboxSound.openSound.soundId
            EarboxSoundState.SOUND_TYPE_CLOSE -> earboxSound.closeSound.soundId
            else -> earboxSound.chargeSound.soundId
        }

        val dialogTitle = when (soundType) {
            EarboxSoundState.SOUND_TYPE_OPEN -> stringRes("device_settings_earbox_opening_sound")
            EarboxSoundState.SOUND_TYPE_CLOSE -> stringRes("device_settings_earbox_closing_sound")
            else -> stringRes("device_settings_earbox_charge_sound")
        }

        AlertDialog(
            onDismissRequest = { activeDialogType = null },
            title = { Text(dialogTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    soundToneNames.forEach { (id, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    controller.setEarboxSoundId(soundType, id)
                                    activeDialogType = null
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                color = if (id == currentSoundId) XiaomiCyan else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (id == currentSoundId) FontWeight.Bold else FontWeight.Normal
                            )
                            if (id == currentSoundId) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = XiaomiCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeDialogType = null }) {
                    Text(stringRes("cancel"))
                }
            }
        )
    }
}
