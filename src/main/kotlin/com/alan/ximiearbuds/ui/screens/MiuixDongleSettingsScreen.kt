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
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.theme.*
import androidx.compose.ui.res.painterResource

/**
 * 1:1 replica of Xiaomi Earbuds `device_settings_fragment_usb.xml` & `DongleSettingsFragment.java`.
 * Uses authentic official drawables, official string resources, and live dongle state bindings.
 */
@Composable
fun MiuixDongleSettingsScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dongleState by controller.dongleConfig.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XiaomiSurface)
    ) {
        MiuixTopAppBar(
            title = stringRes("device_settings_usb_mode"),
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Status Card with Official Dongle Asset
            XiaomiCardContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(XiaomiCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(
                                if (dongleState.dongleInserted) "drawable/device_settings_dongle_settings_icon.png"
                                else "drawable/device_settings_dongle_settings_icon_disable.png"
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Text(
                        text = stringRes("device_settings_usb_mode"),
                        color = XiaomiTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (dongleState.dongleInserted) stringRes("device_settings_device_connected_usb")
                               else stringRes("device_settings_device_disconnected_usb"),
                        color = if (dongleState.dongleInserted) XiaomiGreen else XiaomiTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Group 1: Transmission Modes
            Text(
                text = stringRes("device_settings_function_settings").uppercase(),
                color = XiaomiTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )

            XiaomiCardContainer {
                val modes = listOf(
                    0 to (stringRes("device_settings_usb_mode_lossless_audio") to stringRes("device_settings_spatial_audio_audio_mode")),
                    1 to (stringRes("device_settings_spatial_audio_low_latency") to stringRes("device_settings_low_latency_desc")),
                    2 to (stringRes("device_settings_usb_mode_wireless_mic") to stringRes("device_settings_voice_control"))
                )

                modes.forEachIndexed { index, (modeId, info) ->
                    val (title, subtitle) = info
                    val isSelected = dongleState.dongleMode == modeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { controller.setDongleMode(modeId) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = if (isSelected) XiaomiCyan else XiaomiTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                color = XiaomiTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = XiaomiCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (index < modes.size - 1) {
                        XiaomiItemDivider()
                    }
                }
            }

            // Group 2: Monitoring Volume
            Text(
                text = stringRes("device_settings_dongle_in_ear_monitor").uppercase(),
                color = XiaomiTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )

            XiaomiCardContainer {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringRes("device_settings_dongle_in_ear_monitor"),
                            color = XiaomiTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringRes("device_settings_dongle_in_ear_monitor_click"),
                            color = XiaomiTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = dongleState.monitorSwitch,
                        onCheckedChange = { checked ->
                            controller.setDongleMonitor(checked, dongleState.monitorVolume)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = XiaomiCyan
                        )
                    )
                }

                if (dongleState.monitorSwitch) {
                    XiaomiItemDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringRes("device_settings_notification_volume_title"),
                                color = XiaomiTextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${dongleState.monitorVolume}%",
                                color = XiaomiCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Slider(
                            value = dongleState.monitorVolume.toFloat(),
                            onValueChange = { vol ->
                                controller.setDongleMonitor(true, vol.toInt())
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = XiaomiCyan,
                                activeTrackColor = XiaomiCyan,
                                inactiveTrackColor = XiaomiCardHover
                            )
                        )
                    }
                }
            }
        }
    }
}
