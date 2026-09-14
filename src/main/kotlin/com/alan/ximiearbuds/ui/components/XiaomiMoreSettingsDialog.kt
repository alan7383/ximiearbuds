package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.ui.theme.*

import androidx.compose.material.icons.automirrored.filled.ArrowBack

/**
 * 1:1 Faithful Reproduction of official DeviceSetMoreFragment (device_settings_fragment_set_more.xml)
 */
@Composable
fun XiaomiMoreSettingsView(
    controller: EarbudsController,
    activeModel: EarbudsModel?,
    onBackClicked: () -> Unit,
    onOpenFitDetection: () -> Unit = {},
    onOpenEarboxSound: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val quickSettings by controller.quickSettings.collectAsState()
    val deviceInfo by controller.deviceInfo.collectAsState()
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(deviceInfo.name) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XiaomiPageBg)
    ) {
        // Top Bar: Back Arrow + @string/device_settings_more_settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(XiaomiCardBg)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = XiaomiTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = stringRes("device_settings_more_settings"),
                color = XiaomiTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                    // Section 1: Function Settings Header
                    Text(
                        text = stringRes("device_settings_function_settings"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = XiaomiTextSecondary,
                        modifier = Modifier.padding(start = 6.dp)
                    )

                    XiaomiCardContainer {
                        // In-Ear / Wear Detection
                        if (activeModel?.hasInEarDetection == true) {
                            XiaomiSwitchItem(
                                title = stringRes("device_settings_wear_detection"),
                                subtitle = stringRes("device_settings_wear_detection_des"),
                                checked = quickSettings.inEarDetection,
                                onCheckedChange = { controller.setInEarDetection(it) }
                            )
                        }

                        // Fit Detection (if model supports it)
                        if (activeModel?.hasFitDetection == true) {
                            XiaomiActionItem(
                                title = stringRes("device_settings_fit_detection"),
                                onClick = onOpenFitDetection
                            )
                        }

                        // Ear Canal Detection
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_ear_canal_detection"),
                            checked = quickSettings.earCanalDetection,
                            onCheckedChange = { controller.setEarCanalDetection(it) }
                        )

                        // EarBox Sound (if model supports it)
                        if (activeModel?.hasEarboxSound == true) {
                            XiaomiActionItem(
                                title = stringRes("device_settings_earbox_sound"),
                                onClick = onOpenEarboxSound
                            )
                        }

                        // Dual Device Connection / Multipoint
                        if (activeModel?.hasMultipoint == true) {
                            XiaomiSwitchItem(
                                title = stringRes("device_settings_dual_device_connect"),
                                subtitle = stringRes("device_settings_dual_device_connect_desc"),
                                checked = quickSettings.multipoint,
                                onCheckedChange = { controller.setMultipoint(it) }
                            )
                        }

                        // Auto Pick Call
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_auto_pick_call"),
                            subtitle = stringRes("device_settings_auto_pick_call_suntitle"),
                            checked = quickSettings.autoAnswerPhone,
                            onCheckedChange = { controller.setAutoAnswer(it) }
                        )

                        // Voice Control
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_voice_control"),
                            checked = quickSettings.voiceControl,
                            onCheckedChange = { controller.setVoiceControl(it) }
                        )

                        // Low Latency Mode
                        if (activeModel?.hasLowLatency == true) {
                            XiaomiSwitchItem(
                                title = stringRes("device_settings_spatial_audio_low_latency"),
                                subtitle = stringRes("device_settings_low_latency_desc"),
                                checked = quickSettings.lowLatency,
                                onCheckedChange = { controller.setLowLatency(it) }
                            )
                        }

                        // Notification Bar Display
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_notification_item_title"),
                            subtitle = stringRes("device_settings_notification_item_desc"),
                            checked = quickSettings.notificationBarShow,
                            onCheckedChange = { controller.setNotificationBarShow(it) }
                        )

                        // Hands-free
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_hands_free"),
                            subtitle = stringRes("device_settings_limpid_desc"),
                            checked = quickSettings.handsFree,
                            onCheckedChange = { controller.setHandsFree(it) }
                        )
                    }

                    // Section 2: Others Header
                    Text(
                        text = stringRes("device_settings_others"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = XiaomiTextSecondary,
                        modifier = Modifier.padding(start = 6.dp)
                    )

                    XiaomiCardContainer {
                        // Rename Device
                        XiaomiActionItem(
                            title = stringRes("device_settings_rename_device"),
                            badgeText = deviceInfo.name,
                            onClick = { showRenameDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Remove Device Primary Action Button (style="@style/CommonBtn.Primary")
                    Button(
                        onClick = {
                            controller.removeDevice()
                            onBackClicked()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = stringRes("device_settings_remove_device"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
    }

    // Rename Device Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text(
                    text = stringRes("device_settings_rename_device"),
                    fontWeight = FontWeight.Bold,
                    color = XiaomiTextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(stringRes("device_settings_rename_device")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        controller.renameDevice(renameText)
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = XiaomiOrange)
                ) {
                    Text(stringRes("common_confirm"), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringRes("common_cancel"), color = XiaomiTextSecondary)
                }
            }
        )
    }
}
