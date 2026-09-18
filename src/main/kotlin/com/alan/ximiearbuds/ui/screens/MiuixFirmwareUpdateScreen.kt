package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.CheckCircle
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
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 authentic reproduction of `device_settings_fragment_check_update.xml`
 * (com.mi.earphone.settings.ui.update.CheckUpdateFragment).
 */
@Composable
fun MiuixFirmwareUpdateScreen(
    controller: EarbudsController,
    activeModel: EarbudsModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val otaState by controller.otaState.collectAsState()
    val deviceInfo by controller.deviceInfo.collectAsState()
    val scrollState = rememberScrollState()

    val currentVersion = if (deviceInfo.versionName.isNotBlank()) deviceInfo.versionName else otaState.currentVersion

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XiaomiPageBg)
    ) {
        MiuixTopAppBar(
            title = stringRes("device_settings_firmware_update"),
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(45.dp))
                    .background(XiaomiCyan.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (otaState.isLatest) Icons.Default.CheckCircle else Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = XiaomiCyan,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Model name & version
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = activeModel.commercialName,
                    color = XiaomiTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stringRes("device_settings_device_firmware_version")} $currentVersion",
                    color = XiaomiTextSecondary,
                    fontSize = 13.sp
                )
            }

            // Status message
            Text(
                text = if (otaState.isLatest) {
                    stringRes("app_upgrade_no_newer_version")
                } else {
                    "${stringRes("device_settings_find_fw_new_version")} : v${otaState.latestVersion}"
                },
                color = if (otaState.isLatest) XiaomiGreen else XiaomiTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            // Changelog card
            if (!otaState.isLatest) {
                XiaomiCardContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringRes("device_settings_update_log"),
                            color = XiaomiTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = otaState.changelog,
                            color = XiaomiTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Progress bar if updating
            if (otaState.isUpdating) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { otaState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = XiaomiCyan,
                        trackColor = XiaomiCardBg
                    )
                    Text(
                        text = "${(otaState.progress * 100).toInt()}%",
                        color = XiaomiCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Bottom action button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = XiaomiCardBg,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = {
                        if (!otaState.isUpdating && !otaState.isLatest) {
                            controller.startOtaUpdate()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (otaState.isLatest) Color(0x33888888) else XiaomiCyan
                    ),
                    enabled = !otaState.isUpdating
                ) {
                    Text(
                        text = when {
                            otaState.isUpdating -> stringRes("appupgrade_upgradeing")
                            otaState.isLatest -> stringRes("app_upgrade_no_newer_version")
                            else -> stringRes("device_settings_update_now")
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
