package com.alan.ximiearbuds.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.bluetooth.ConnectionState
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 Reproduction of official ScanDeviceFragment (device_fragment_scan_device.xml).
 * Shows the authentic pairing instructions, large device render (231dp max),
 * pulsating scanning dots, "Rechercher à nouveau" action button and "Aide à la recherche" link.
 */
@Composable
fun XiaomiScanDeviceView(
    model: EarbudsModel,
    controller: EarbudsController,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isScanning by controller.isScanning.collectAsState()
    val discoveredDevices by controller.discoveredDevices.collectAsState()
    val pairedDevices by controller.pairedDevices.collectAsState()
    val connectionState by controller.connectionState.collectAsState()

    // Keep Bluetooth scanning active while on this screen
    DisposableEffect(Unit) {
        controller.startScan()
        onDispose {
            controller.stopScan()
        }
    }

    // Identify discovered or paired device matching the selected model
    val matchingDevice = remember(discoveredDevices, pairedDevices) {
        (discoveredDevices + pairedDevices).firstOrNull { dev ->
            dev.name.contains(model.commercialName, ignoreCase = true) ||
            model.commercialName.contains(dev.name, ignoreCase = true) ||
            (model.codename.contains("O76", ignoreCase = true) && dev.name.contains("Buds 6 Pro", ignoreCase = true)) ||
            (dev.name.contains("Redmi", ignoreCase = true) && dev.name.contains("Buds", ignoreCase = true)) ||
            (dev.isXiaomiEarbuds && dev.name.contains(model.brand, ignoreCase = true))
        }
    }

    // Auto-connect once detected
    LaunchedEffect(matchingDevice) {
        if (matchingDevice != null && connectionState == ConnectionState.DISCONNECTED) {
            controller.connectToDevice(matchingDevice)
        }
    }

    // Determine specific pairing instructions by model codename
    val descText = when {
        model.codename.contains("M79A", ignoreCase = true) -> stringRes("device_manager_scan_desc_m79a")
        model.codename.contains("J77S", ignoreCase = true) -> stringRes("device_manager_scan_desc_j77s")
        model.codename.contains("O73", ignoreCase = true) -> stringRes("device_manager_scan_desc_o73")
        model.codename.contains("O70C", ignoreCase = true) -> stringRes("device_manager_scan_desc_o70c")
        else -> stringRes("device_manager_scan_desc")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XiaomiPageBg)
    ) {
        // -------------------------------------------------------------------------
        // 1. Top Bar: Back Arrow + @string/device_add_title ("Ajouter des écouteurs")
        // -------------------------------------------------------------------------
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
                text = stringRes("device_add_title"),
                color = XiaomiTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // -------------------------------------------------------------------------
        // 2. Central Device Connection Layout (device_connect_layout)
        // -------------------------------------------------------------------------
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large Device Render (device_icon, max 231dp)
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { controller.connectVirtualModel(model) }
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                val activeDeviceInfo by controller.deviceInfo.collectAsState()
                XiaomiDeviceImage(
                    model = model,
                    colorType = if (activeDeviceInfo.colorType > 0) activeDeviceInfo.colorType else model.defaultColor,
                    contentDescription = model.commercialName,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Model Name (device_name_tv, FontMedium.20sp)
            Text(
                text = model.commercialName,
                color = XiaomiTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pairing Instructions (device_desc_tv, FontRegular.14sp)
            Text(
                text = descText,
                color = XiaomiTextSecondary,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (matchingDevice != null) {
                Spacer(modifier = Modifier.height(18.dp))
                // Discovered Device Match Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = XiaomiCardBg,
                    border = BorderStroke(1.dp, XiaomiCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (connectionState == ConnectionState.CONNECTING) XiaomiOrange else XiaomiCyan)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = matchingDevice.name.ifBlank { model.commercialName },
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = XiaomiTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (connectionState == ConnectionState.CONNECTING) stringRes("device_manager_connecting") else matchingDevice.address,
                                fontSize = 12.sp,
                                color = if (connectionState == ConnectionState.CONNECTING) XiaomiCyan else XiaomiTextMuted
                            )
                        }
                        Button(
                            onClick = { controller.connectToDevice(matchingDevice) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = XiaomiCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp),
                            enabled = connectionState != ConnectionState.CONNECTING
                        ) {
                            Text(
                                text = if (connectionState == ConnectionState.CONNECTING) "…" else stringRes("device_connect_device"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(28.dp))

                // Loading / Scanning Indicator (device_lav_scan_device_scanning)
                if (isScanning) {
                    ScanningDotsAnimation()
                } else {
                    Text(
                        text = stringRes("device_manager_scan_none"),
                        color = XiaomiTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // -------------------------------------------------------------------------
        // 3. Bottom Action Buttons (scan_again_tv & device_tv_scan_device_help)
        // -------------------------------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (matchingDevice != null) {
                Button(
                    onClick = { controller.connectToDevice(matchingDevice) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = XiaomiCyan,
                        contentColor = Color.Black
                    ),
                    enabled = connectionState != ConnectionState.CONNECTING
                ) {
                    Text(
                        text = if (connectionState == ConnectionState.CONNECTING) stringRes("device_manager_connecting") else "${stringRes("device_connect_device")} ${matchingDevice.name}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                // "Rechercher à nouveau" Button (BaseButton.Positive)
                Button(
                    onClick = { controller.startScan() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = XiaomiBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringRes("device_manager_scan_again"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Demo Mode fallback button
            TextButton(
                onClick = { controller.connectVirtualModel(model) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringRes("device_manager_demo_mode"),
                    color = XiaomiTextMuted,
                    fontSize = 12.5.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // "Aide à la recherche" Link (device_tv_scan_device_help)
            Text(
                text = stringRes("device_search_help"),
                color = XiaomiBlue,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { /* help info dialog */ }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun ScanningDotsAnimation() {
    val transition = rememberInfiniteTransition()
    val dot1Alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val dot2Alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val dot3Alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(XiaomiCyan.copy(alpha = dot1Alpha))
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(XiaomiCyan.copy(alpha = dot2Alpha))
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(XiaomiCyan.copy(alpha = dot3Alpha))
        )
    }
}
