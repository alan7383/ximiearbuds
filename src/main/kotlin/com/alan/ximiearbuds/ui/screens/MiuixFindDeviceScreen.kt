package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
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
import com.alan.ximiearbuds.core.protocol.RingTarget
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiActionItem
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.theme.XiaomiCyan
import com.alan.ximiearbuds.ui.theme.XiaomiRed
import com.alan.ximiearbuds.ui.theme.stringRes

/**
 * 1:1 replica of Xiaomi Earbuds `device_settings_fragment_find_device.xml`.
 * Fully wired to official MIUI strings.
 */
@Composable
fun MiuixFindDeviceScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ringingLeft by remember { mutableStateOf(false) }
    var ringingRight by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Navigation Bar
        MiuixTopAppBar(
            title = stringRes("device_settings_find_device"),
            onBackClick = {
                if (ringingLeft || ringingRight) {
                    controller.stopRinging()
                }
                onBackClick()
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(XiaomiCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = XiaomiCyan,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringRes("device_settings_find"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Warning Box (device_settings_find_desc)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x1AFF9500))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringRes("device_settings_find_desc"),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Chime Controls Card
            XiaomiCardContainer(modifier = Modifier.fillMaxWidth()) {
                // Left Earbud Chime
                XiaomiActionItem(
                    title = stringRes("device_settings_find_side_left"),
                    subtitle = if (ringingLeft) stringRes("device_settings_find_left_playing") else stringRes("login_guide_find_earphone_detail"),
                    onClick = {
                        ringingLeft = !ringingLeft
                        if (ringingLeft) {
                            controller.ringEarbuds(RingTarget.LEFT)
                        } else {
                            controller.stopRinging()
                        }
                    }
                )

                XiaomiItemDivider()

                // Right Earbud Chime
                XiaomiActionItem(
                    title = stringRes("device_settings_find_side_right"),
                    subtitle = if (ringingRight) stringRes("device_settings_find_right_playing") else stringRes("login_guide_find_earphone_detail"),
                    onClick = {
                        ringingRight = !ringingRight
                        if (ringingRight) {
                            controller.ringEarbuds(RingTarget.RIGHT)
                        } else {
                            controller.stopRinging()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Ring Both Button
            Button(
                onClick = {
                    val nextState = !(ringingLeft && ringingRight)
                    ringingLeft = nextState
                    ringingRight = nextState
                    if (nextState) {
                        controller.ringEarbuds(RingTarget.BOTH)
                    } else {
                        controller.stopRinging()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (ringingLeft || ringingRight) XiaomiRed else XiaomiCyan,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (ringingLeft || ringingRight) stringRes("device_settings_audio_play_status_pause") else stringRes("device_settings_find"),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
