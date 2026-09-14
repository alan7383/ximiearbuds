package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alan.ximiearbuds.ui.theme.*

@Composable
fun XiaomiFindDeviceDialog(
    onRingLeft: (Boolean) -> Unit,
    onRingRight: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var isLeftRinging by remember { mutableStateOf(false) }
    var isRightRinging by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(XiaomiCardBg)
                .border(1.dp, XiaomiCardBorder, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringRes("device_settings_find_device"),
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

                // Warning Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1AF04D18))
                        .border(1.dp, Color(0x33F04D18), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringRes("device_settings_find_play_warn"),
                        color = XiaomiOrange,
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Two Earbud Ring Buttons (Left / Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Left Earbud Ring
                    RingBudCard(
                        title = stringRes("device_settings_left"),
                        isRinging = isLeftRinging,
                        onToggle = {
                            isLeftRinging = !isLeftRinging
                            onRingLeft(isLeftRinging)
                        }
                    )

                    // Right Earbud Ring
                    RingBudCard(
                        title = stringRes("device_settings_right"),
                        isRinging = isRightRinging,
                        onToggle = {
                            isRightRinging = !isRightRinging
                            onRingRight(isRightRinging)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RingBudCard(
    title: String,
    isRinging: Boolean,
    onToggle: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141416))
            .clickable(onClick = onToggle)
            .padding(18.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isRinging) XiaomiOrange else Color(0xFF222226)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource("drawable/device_settings_ic_find_device.webp"),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            color = XiaomiTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isRinging) stringRes("device_settings_find_playing") else stringRes("device_settings_find_play"),
            color = if (isRinging) XiaomiOrange else XiaomiCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
