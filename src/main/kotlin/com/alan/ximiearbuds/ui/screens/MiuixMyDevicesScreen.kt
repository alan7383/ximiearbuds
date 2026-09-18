package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.bluetooth.ConnectionState
import com.alan.ximiearbuds.core.bluetooth.DiscoveredDevice
import com.alan.ximiearbuds.core.device.DevicePreferences
import com.alan.ximiearbuds.core.device.DeviceRegistry
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiDeviceImage
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 reproduction of Xiaomi Earbuds `DeviceListFragment` (device_fragment_device_list.xml & device_item_device_list.xml).
 * Title: "Mes écouteurs" (@string/device_my_devices).
 */
@Composable
fun MiuixMyDevicesScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    onNavigateToAddDevice: () -> Unit,
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    val pairedDevices by controller.pairedDevices.collectAsState()
    val connectionState by controller.connectionState.collectAsState()
    val isConnected = connectionState == ConnectionState.CONNECTED
    val isConnecting = connectionState == ConnectionState.CONNECTING
    val activeModel by controller.activeModel.collectAsState()
    val deviceInfo by controller.deviceInfo.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Synthesize current connected / active device into the list if not already present
    val currentDevice = remember(isConnected, activeModel, deviceInfo) {
        if (isConnected || activeModel != null) {
            DiscoveredDevice(
                name = activeModel?.commercialName ?: deviceInfo.name.ifBlank { "Xiaomi Earbuds" },
                address = deviceInfo.address.ifBlank { "00:BB:43:8B:C0:F3" },
                isConnected = isConnected,
                isXiaomiEarbuds = true,
                colorType = deviceInfo.colorType
            )
        } else null
    }

    val displayDevices = remember(pairedDevices, currentDevice, isConnected) {
        val list = pairedDevices.toMutableList()
        if (currentDevice != null) {
            val idx = list.indexOfFirst { it.address.equals(currentDevice.address, ignoreCase = true) }
            if (idx >= 0) {
                list[idx] = list[idx].copy(isConnected = isConnected, name = currentDevice.name)
            } else {
                list.add(0, currentDevice)
            }
        }
        list.sortedByDescending { it.isConnected }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Miuix Action Bar with title @string/device_my_devices ("Mes écouteurs")
            MiuixTopAppBar(
                title = stringRes("device_my_devices"),
                onBackClick = onBackClick
            )

            if (displayDevices.isEmpty()) {
                // Empty state: device_list_empty_text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 60.dp)
                    ) {
                        Image(
                            painter = painterResource(
                                if (isDark) "drawable/device_list_empty_night.webp"
                                else "drawable/device_list_empty.webp"
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(140.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = stringRes("device_no_paired_device_tip"),
                            fontSize = 14.sp,
                            color = XiaomiTextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                // Device List matching device_item_device_list.xml (133.3dp cards)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(displayDevices, key = { it.address }) { device ->
                        DeviceListItemCard(
                            device = device,
                            isCurrentlyConnecting = isConnecting && device.isConnected,
                            onCardClick = {
                                onDeviceSelected(device)
                            },
                            onSwitchClick = {
                                if (device.isConnected) {
                                    controller.disconnect()
                                } else {
                                    controller.connectToDevice(device)
                                    onDeviceSelected(device)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Add Button (@drawable/device_list_add_btn.png) aligned bottom-end
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
                .size(60.dp)
                .clip(CircleShape)
                .clickable(onClick = onNavigateToAddDevice)
        ) {
            Image(
                painter = painterResource("drawable/device_list_add_btn.png"),
                contentDescription = stringRes("device_add_title"),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 1:1 reproduction of `device_item_device_list.xml`:
 * - Height: ~133.3dp
 * - Background: selector_card_bg (XiaomiCardBg with 16dp rounded corners)
 * - Left: device_icon_iv (97dp x 97dp)
 * - Center: device_name_tv (16sp medium) + device_des_tv (12sp regular)
 * - Right: switch_device_btn (height 38.67dp, minWidth 70dp)
 */
@Composable
private fun DeviceListItemCard(
    device: DiscoveredDevice,
    isCurrentlyConnecting: Boolean,
    onCardClick: () -> Unit,
    onSwitchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val model = remember(device.name) {
        if (device.isXiaomiEarbuds) DeviceRegistry.findByName(device.name) else null
    }
    val effectiveModel = if (model != null && model.codename != "GENERIC") model else null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(133.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(XiaomiCardBg)
            .border(1.dp, XiaomiCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onCardClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // device_icon_iv: 97dp x 97dp
        Box(
            modifier = Modifier
                .size(97.dp),
            contentAlignment = Alignment.Center
        ) {
            if (effectiveModel != null) {
                XiaomiDeviceImage(
                    model = effectiveModel,
                    colorType = device.colorType,
                    modifier = Modifier.size(86.dp)
                )
            } else {
                Image(
                    painter = painterResource("drawable/device_list_icon_default.webp"),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Center Column: device_name_tv + device_des_tv
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = device.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = XiaomiTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            val statusText = when {
                isCurrentlyConnecting -> stringRes("device_manager_connecting")
                device.isConnected -> stringRes("device_connected")
                else -> stringRes("device_disconnected")
            }
            val statusColor = if (device.isConnected) XiaomiCyan else XiaomiTextMuted

            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // switch_device_btn: 38.67dp height, minWidth 70dp, rounded pill
        // If connected: device_list_status_connect_bg (#17222d) with "Déconnecter" in Cyan
        // If disconnected: device_list_status_normal_bg (#272727) with "Connecter" in White
        val btnBg = if (device.isConnected) Color(0xFF17222D) else Color(0xFF272727)
        val btnTextColor = if (device.isConnected) XiaomiCyan else XiaomiTextPrimary
        val btnLabel = if (device.isConnected) {
            stringRes("device_disconnect_device")
        } else {
            stringRes("device_connect_device")
        }

        Surface(
            onClick = onSwitchClick,
            shape = RoundedCornerShape(20.dp),
            color = btnBg,
            modifier = Modifier
                .height(38.dp)
                .defaultMinSize(minWidth = 72.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                Text(
                    text = btnLabel,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = btnTextColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
