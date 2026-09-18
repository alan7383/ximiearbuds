package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiActionItem
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.components.XiaomiSwitchItem
import com.alan.ximiearbuds.ui.theme.XiaomiCyan
import com.alan.ximiearbuds.ui.theme.XiaomiRed
import com.alan.ximiearbuds.ui.theme.XiaomiTextPrimary
import com.alan.ximiearbuds.ui.theme.XiaomiTextSecondary
import com.alan.ximiearbuds.ui.theme.stringRes

/**
 * 1:1 replica of Xiaomi Earbuds `device_settings_fragment_set_more.xml`.
 * Fully wired to official MIUI strings and dialogs.
 */
@Composable
fun MiuixMoreSettingsScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    onNavigateToEarbox: () -> Unit,
    onNavigateToFitDetection: () -> Unit,
    onNavigateToDeviceInfo: () -> Unit,
    onNavigateToDongle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val quickSettings by controller.quickSettings.collectAsState()
    val deviceInfo by controller.deviceInfo.collectAsState()
    val activeModel by controller.activeModel.collectAsState()
    val model = activeModel ?: com.alan.ximiearbuds.core.device.DeviceRegistry.GENERIC_MODEL
    val scrollState = rememberScrollState()

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showRemoveDialog by remember { mutableStateOf(false) }
    val voiceHotword by controller.voiceHotword.collectAsState()
    val callListenerSeconds by controller.callListenerSeconds.collectAsState()
    val speechToChat = callListenerSeconds > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Authentic Top Navigation Bar
        MiuixTopAppBar(
            title = stringRes("device_settings_more_settings"),
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

            // Section Header: Paramètres des fonctionnalités (label_function) - Group 3
            if (model.hasGroup(com.alan.ximiearbuds.core.protocol.OfficialGroupIds.GROUP_FUNCTION_SETTING)) {
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
                    var hasPrevious = false

                    // Speech-to-Chat / Conversation libre (handsFreeView - 3001 / Config 13)
                    if (model.hasFunction(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_SMART_FREE_PICK)) {
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_hands_free"),
                            subtitle = stringRes("device_settings_limpid_desc"),
                            checked = speechToChat,
                            onCheckedChange = { controller.setCallListener(if (it) 5 else 0) }
                        )
                        hasPrevious = true
                    }

                    // In-Ear Wear Detection (monitorView - 3002)
                    if (model.moreSettingsCapabilities.hasWearDetection || model.hasFunction(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_WEAR_DETECTION)) {
                        if (hasPrevious) XiaomiItemDivider()
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_wear_detection"),
                            subtitle = stringRes("device_settings_wear_detection_des"),
                            checked = quickSettings.inEarDetection,
                            onCheckedChange = { controller.setInEarDetection(it) }
                        )
                        hasPrevious = true
                    }

                    // Dual Device Multipoint (multy_connect_View - 3004)
                    if (model.moreSettingsCapabilities.hasMultipoint || model.hasFunction(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_DUAL_DEVICE_CONNECTION)) {
                        if (hasPrevious) XiaomiItemDivider()
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_dual_device_connect"),
                            subtitle = stringRes("device_settings_dual_device_connect_desc"),
                            checked = quickSettings.multipoint,
                            onCheckedChange = { controller.setMultipoint(it) }
                        )
                        hasPrevious = true
                    }

                    // Low Latency Gaming Mode (low_latency - 3011 / 3009)
                    if (model.moreSettingsCapabilities.hasLowLatency || model.hasFunction(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_LOW_LATENCY)) {
                        if (hasPrevious) XiaomiItemDivider()
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_spatial_audio_low_latency"),
                            subtitle = stringRes("device_settings_low_latency_desc"),
                            checked = quickSettings.lowLatency,
                            onCheckedChange = { controller.setLowLatency(it) }
                        )
                        hasPrevious = true
                    }

                    // Auto Pick Call (3008 / Config 3)
                    if (model.moreSettingsCapabilities.hasAutoPickCall || model.hasFunction(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_AUTO_PICK_CALL)) {
                        if (hasPrevious) XiaomiItemDivider()
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_auto_pick_call"),
                            subtitle = stringRes("device_settings_auto_pick_call_suntitle"),
                            checked = quickSettings.autoAnswerPhone,
                            onCheckedChange = { controller.setAutoAnswer(it) }
                        )
                        hasPrevious = true
                    }

                    // Voice Control Hotword (3005 / VendorData type 2 / Config 126)
                    if (model.moreSettingsCapabilities.hasVoiceControl || model.hasFunction(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_VOICE_CONTROL)) {
                        if (hasPrevious) XiaomiItemDivider()
                        XiaomiSwitchItem(
                            title = stringRes("device_settings_voice_control"),
                            subtitle = stringRes("device_settings_voice_control_detail"),
                            checked = voiceHotword,
                            onCheckedChange = { controller.setVoiceHotword(it) }
                        )
                        hasPrevious = true
                    }

                    // Fit Detection (fitDetect - 3003)
                    if (model.moreSettingsCapabilities.hasFitDetection || model.hasFunction(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_FIT_DETECT)) {
                        if (hasPrevious) XiaomiItemDivider()
                        XiaomiActionItem(
                            title = stringRes("device_settings_fit_detection"),
                            subtitle = stringRes("login_guide_fit_detect_detail"),
                            onClick = onNavigateToFitDetection
                        )
                        hasPrevious = true
                    }

                    // Ear Canal Detection (3013)
                    if (model.moreSettingsCapabilities.hasEarCanalDetection || model.hasFunction(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_EAR_CANAL_DETECTION)) {
                        if (hasPrevious) XiaomiItemDivider()
                        XiaomiActionItem(
                            title = stringRes("device_settings_ear_canal_detection"),
                            subtitle = stringRes("device_settings_ear_canal_detection_desc"),
                            onClick = onNavigateToFitDetection
                        )
                        hasPrevious = true
                    }

                    // Earbox Sound (earbox - 3015)
                    if (model.moreSettingsCapabilities.hasEarboxSound || model.hasFunction(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_EARBOX_SOUND)) {
                        if (hasPrevious) XiaomiItemDivider()
                        XiaomiActionItem(
                            title = stringRes("device_settings_earbox_sound"),
                            subtitle = stringRes("device_settings_earbox_charge_sound"),
                            onClick = onNavigateToEarbox
                        )
                        hasPrevious = true
                    }

                    // Dongle mode (7001 or Group 7)
                    if ((model.moreSettingsCapabilities.hasDongle || model.hasFunction(com.alan.ximiearbuds.core.protocol.OfficialFunctionIds.FUNC_USB_MODE)) && onNavigateToDongle != null) {
                        if (hasPrevious) XiaomiItemDivider()
                        XiaomiActionItem(
                            title = stringRes("device_settings_usb_mode"),
                            subtitle = stringRes("device_settings_dongle_gesture_notify"),
                            onClick = onNavigateToDongle
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section Header: Autre (label_other)
            Text(
                text = stringRes("device_settings_others").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                // Rename Device (renameDevice)
                XiaomiActionItem(
                    title = stringRes("device_settings_rename_device"),
                    subtitle = deviceInfo.name.ifBlank { "Xiaomi Earbuds" },
                    onClick = {
                        renameText = deviceInfo.name.ifBlank { "Xiaomi Earbuds" }
                        showRenameDialog = true
                    }
                )

                XiaomiItemDivider()

                // Device Info
                XiaomiActionItem(
                    title = stringRes("device_settings_about_device"),
                    subtitle = deviceInfo.versionName.ifBlank { "v1.0.8.2" },
                    onClick = onNavigateToDeviceInfo
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Unpair Device Destructive Button (delete_device)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { showRemoveDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = XiaomiRed.copy(alpha = 0.12f),
                        contentColor = XiaomiRed
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = stringRes("device_settings_remove_device"),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Rename Device Dialog (matching SettingsPageUtil.showRenameDeviceDialog)
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text(
                    text = stringRes("device_settings_rename_device"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text(stringRes("device_settings_rename_device")) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                    }
                ) {
                    Text(
                        text = stringRes("device_settings_rename_dialog_confirm"),
                        color = XiaomiCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(
                        text = stringRes("device_settings_dialog_cancle"),
                        color = XiaomiTextSecondary
                    )
                }
            }
        )
    }

    // Remove / Unpair Device Dialog (matching SettingsPageUtil.showRemoveDeviceDialog)
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = {
                Text(
                    text = stringRes("device_settings_remove_device"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = stringRes("device_settings_remove_device_des"),
                    fontSize = 14.sp,
                    color = XiaomiTextPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        controller.disconnect()
                        onBackClick()
                    }
                ) {
                    Text(
                        text = stringRes("device_settings_remove_device_confirm"),
                        color = XiaomiRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(
                        text = stringRes("device_settings_dialog_cancle"),
                        color = XiaomiTextSecondary
                    )
                }
            }
        )
    }
}
