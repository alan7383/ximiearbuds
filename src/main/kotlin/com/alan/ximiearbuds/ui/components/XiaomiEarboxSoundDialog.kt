package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 Faithful Reproduction of official EarBox Sound Settings (device_settings_fragment_earbox_sound.xml)
 */
@Composable
fun XiaomiEarboxSoundDialog(
    onDismiss: () -> Unit
) {
    var chargeSoundEnabled by remember { mutableStateOf(true) }
    var openSoundEnabled by remember { mutableStateOf(true) }
    var closeSoundEnabled by remember { mutableStateOf(true) }
    var earboxVolume by remember { mutableStateOf(80) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(500.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = XiaomiPageBg,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringRes("device_settings_earbox_sound"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = XiaomiTextPrimary
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sounds Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Charge Sound Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(XiaomiCardBg)
                                .clickable { chargeSoundEnabled = !chargeSoundEnabled }
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Image(
                                    painter = painterResource("drawable/device_settings_charge_icon.png"),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                                Column {
                                    Text(
                                        text = stringRes("device_settings_earbox_charge_sound"),
                                        color = XiaomiTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (chargeSoundEnabled) stringRes("common_enable") else stringRes("common_disable"),
                                        color = if (chargeSoundEnabled) XiaomiOrange else XiaomiTextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Open & Close Sounds Stack
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Open Sound Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(75.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(XiaomiCardBg)
                                    .clickable { openSoundEnabled = !openSoundEnabled }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource("drawable/device_settings_opening_sound.png"),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringRes("device_settings_earbox_opening_sound"),
                                        color = XiaomiTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (openSoundEnabled) stringRes("common_enable") else stringRes("common_disable"),
                                        color = if (openSoundEnabled) XiaomiOrange else XiaomiTextMuted,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            // Close Sound Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(75.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(XiaomiCardBg)
                                    .clickable { closeSoundEnabled = !closeSoundEnabled }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource("drawable/device_settings_closing_sound.png"),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringRes("device_settings_earbox_closing_sound"),
                                        color = XiaomiTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (closeSoundEnabled) stringRes("common_enable") else stringRes("common_disable"),
                                        color = if (closeSoundEnabled) XiaomiOrange else XiaomiTextMuted,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }

                    // Volume Control Section
                    Text(
                        text = stringRes("device_settings_earbox_volume_control"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = XiaomiTextSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    XiaomiCardContainer {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = earboxVolume.toFloat(),
                                onValueChange = { earboxVolume = it.toInt() },
                                valueRange = 0f..100f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = XiaomiOrange,
                                    activeTrackColor = XiaomiOrange
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "$earboxVolume%",
                                fontSize = 13.sp,
                                color = XiaomiTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
