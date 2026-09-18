package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.theme.XiaomiCyan
import com.alan.ximiearbuds.ui.theme.XiaomiRed
import com.alan.ximiearbuds.ui.theme.stringRes
import androidx.compose.ui.res.painterResource

/**
 * 1:1 replica of Xiaomi Earbuds `device_settings_fragment_find_device.xml` & `FindDeviceFragment.java`.
 * Uses authentic official drawables, string resources, and the safety confirmation dialog.
 */
@Composable
fun MiuixFindDeviceScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val findDeviceState by controller.findDevice.collectAsState()
    val ringingLeft = findDeviceState.isRingingLeft
    val ringingRight = findDeviceState.isRingingRight
    val isAnyRinging = ringingLeft || ringingRight
    val scrollState = rememberScrollState()

    var showSafetyDialog by remember { mutableStateOf(false) }
    var pendingRingTarget by remember { mutableStateOf<RingTarget?>(null) }
    var hasConfirmedSafetyWarning by remember { mutableStateOf(false) }

    fun triggerRing(target: RingTarget) {
        if (!hasConfirmedSafetyWarning) {
            pendingRingTarget = target
            showSafetyDialog = true
        } else {
            when (target) {
                RingTarget.LEFT -> controller.ringEarbud(RingTarget.LEFT, !ringingLeft)
                RingTarget.RIGHT -> controller.ringEarbud(RingTarget.RIGHT, !ringingRight)
                RingTarget.BOTH -> controller.ringEarbuds(RingTarget.BOTH)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Navigation Bar
        MiuixTopAppBar(
            title = stringRes("device_settings_find_device"),
            onBackClick = {
                if (isAnyRinging) {
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
            // Official Earbuds Acoustic Ring Graphic
            val heroIcon = when {
                ringingLeft && ringingRight -> "drawable/device_settings_find_device_icon_both_selected.webp"
                ringingLeft -> "drawable/device_settings_find_device_icon_left_selected.webp"
                ringingRight -> "drawable/device_settings_find_device_icon_right_selected.webp"
                else -> "drawable/device_settings_find_device_icon_both_normal.webp"
            }

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(48.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(heroIcon),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringRes("device_settings_find"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Warning Box: device_settings_find_warn (Official warning)
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
                        text = stringRes("device_settings_find_warn"),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Chime Controls Card with official illustrations (device_settings_item_find_device.xml)
            XiaomiCardContainer(modifier = Modifier.fillMaxWidth()) {
                // Left Earbud Item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { triggerRing(RingTarget.LEFT) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(
                            if (ringingLeft) "drawable/device_settings_find_device_icon_left_selected.webp"
                            else "drawable/device_settings_find_device_icon_left_normal.webp"
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringRes("device_settings_find_side_left"),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (ringingLeft) stringRes("device_settings_find_left_playing") else stringRes("device_settings_find_ring"),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ringingLeft) XiaomiCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                XiaomiItemDivider()

                // Right Earbud Item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { triggerRing(RingTarget.RIGHT) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(
                            if (ringingRight) "drawable/device_settings_find_device_icon_right_selected.webp"
                            else "drawable/device_settings_find_device_icon_right_normal.webp"
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringRes("device_settings_find_side_right"),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (ringingRight) stringRes("device_settings_find_right_playing") else stringRes("device_settings_find_ring"),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ringingRight) XiaomiCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Ring Both Earbuds Button
            Button(
                onClick = {
                    if (isAnyRinging) {
                        controller.stopRinging()
                    } else {
                        triggerRing(RingTarget.BOTH)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAnyRinging) XiaomiRed else XiaomiCyan,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isAnyRinging) stringRes("device_settings_find_sides_playing") else stringRes("device_settings_find_sides"),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Official Safety Reminder Dialog (FindDeviceFragment showRemindDialog)
    if (showSafetyDialog) {
        AlertDialog(
            onDismissRequest = {
                showSafetyDialog = false
                pendingRingTarget = null
            },
            title = {
                Text(
                    text = stringRes("device_settings_find_audio_warn"),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = stringRes("device_settings_find_play_warn"),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        hasConfirmedSafetyWarning = true
                        showSafetyDialog = false
                        pendingRingTarget?.let { target ->
                            when (target) {
                                RingTarget.LEFT -> controller.ringEarbud(RingTarget.LEFT, true)
                                RingTarget.RIGHT -> controller.ringEarbud(RingTarget.RIGHT, true)
                                RingTarget.BOTH -> controller.ringEarbuds(RingTarget.BOTH)
                            }
                        }
                        pendingRingTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = XiaomiCyan)
                ) {
                    Text(stringRes("device_settings_find_play"), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSafetyDialog = false
                        pendingRingTarget = null
                    }
                ) {
                    Text(stringRes("cancel"))
                }
            }
        )
    }
}
