package com.alan.ximiearbuds.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.bluetooth.DiscoveredDevice
import com.alan.ximiearbuds.core.device.DeviceRegistry
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 Strict Reproduction of official AddDeviceFragment (device_fragment_add_device.xml)
 * and AddDeviceViewModel.java from decompiled Xiaomi Earbuds APK.
 *
 * Exact official visual elements:
 * 1. Title bar with back arrow and @string/device_add_title ("Ajouter des écouteurs")
 * 2. Scanning section (device_item_bluetooth_connect.xml) with radar pulse & @string/device_bluetooth_connect_hint
 * 3. Nearby discovered Bluetooth devices list (device_item_device_list.xml) with "Connecter" button
 * 4. Section divider with @string/device_add_self ("Ajouter des écouteurs manuellement")
 * 5. 2-column grid of official device cards (device_item_small_device_card.xml):
 *    - 167dp height, 20dp corner radius (device_scan_item_bg)
 *    - 96dp x 96dp authentic device render image
 *    - FontMedium 16sp title (device_name_tv)
 *    - FontRegular 12sp subtitle (device_edition_tv, 40% text color)
 *
 * NO artificial desktop additions (search bar, filter chips removed).
 */
@Composable
fun XiaomiAddDeviceView(
    controller: EarbudsController,
    onBackClicked: () -> Unit,
    onModelSelected: (EarbudsModel) -> Unit,
    onDeviceSelected: ((DiscoveredDevice) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isScanning by controller.isScanning.collectAsState()
    val discoveredDevices by controller.discoveredDevices.collectAsState()
    val pairedDevices by controller.pairedDevices.collectAsState()
    val allModels by DeviceRegistry.modelsFlow.collectAsState()

    // Start Bluetooth scanning on view entry, stop on exit (replicates AddDeviceFragment.onResume/onPause)
    DisposableEffect(Unit) {
        controller.startScan()
        onDispose {
            controller.stopScan()
        }
    }

    // Nearby discovered and paired Xiaomi earbuds, deduplicated by MAC address
    val allVisibleDevices = remember(discoveredDevices, pairedDevices) {
        val map = LinkedHashMap<String, DiscoveredDevice>()
        for (d in discoveredDevices) {
            if (d.isXiaomiEarbuds) map[d.address] = d
        }
        for (d in pairedDevices) {
            if (d.isXiaomiEarbuds && !map.containsKey(d.address)) map[d.address] = d
        }
        map.values.toList()
    }

    // Deduplicate models by commercial name (replicates handleDeviceList() in AddDeviceViewModel.java)
    val catalogModels = remember(allModels) {
        val seenNames = HashSet<String>()
        val result = mutableListOf<EarbudsModel>()
        for (model in allModels) {
            if (model.codename == "GENERIC") continue
            if (seenNames.add(model.commercialName)) {
                result.add(model)
            }
        }
        result
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
        // 2. Scrollable Body: Replicates RecyclerView in device_fragment_add_device.xml
        // -------------------------------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // A. Section Title: "Recherche des écouteurs à proximité…" (device_searching_btn_text)
            Text(
                text = stringRes("device_searching_btn_text"),
                color = XiaomiTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 12.dp)
            )

            // B. Scanning Radar Item (device_item_bluetooth_connect.xml)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(XiaomiCardBg)
                    .border(width = 1.dp, color = XiaomiCardBorder, shape = RoundedCornerShape(20.dp))
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OfficialRadarScanner(isScanning = isScanning)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringRes("device_bluetooth_connect_hint"),
                        fontSize = 12.5.sp,
                        color = XiaomiTextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // C. Discovered Nearby Devices (device_item_device_list.xml)
            if (allVisibleDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                allVisibleDevices.forEach { device ->
                    DiscoveredDeviceItemCard(
                        device = device,
                        onConnectClicked = {
                            controller.connectToDevice(device)
                            onDeviceSelected?.invoke(device)
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // D. Section Divider with Title: "Ajouter des écouteurs manuellement" (device_add_self)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringRes("device_add_self"),
                    color = XiaomiTextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.width(12.dp))
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = XiaomiDivider,
                    thickness = 0.8.dp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // E. 2-Column Grid: Official device_item_small_device_card.xml
            val chunkedModels = remember(catalogModels) {
                catalogModels.chunked(2)
            }

            chunkedModels.forEach { rowModels ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1
                    SmallDeviceCard(
                        model = rowModels[0],
                        modifier = Modifier.weight(1f),
                        onClick = { onModelSelected(rowModels[0]) }
                    )

                    // Card 2 or empty placeholder for balance
                    if (rowModels.size > 1) {
                        SmallDeviceCard(
                            model = rowModels[1],
                            modifier = Modifier.weight(1f),
                            onClick = { onModelSelected(rowModels[1]) }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 1:1 Reproduction of device_item_small_device_card.xml
 * ConstraintLayout with height 167dp, device_scan_item_bg (20dp corner radius),
 * device_icon_iv (96dp x 96dp), device_name_tv (16sp medium), and device_edition_tv (12sp regular 40%).
 */
@Composable
private fun SmallDeviceCard(
    model: EarbudsModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(167.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(XiaomiCardBg)
            .border(width = 1.dp, color = XiaomiCardBorder, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Authentic 96dp x 96dp Device Render (device_icon_iv)
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .padding(bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                XiaomiDeviceImage(
                    model = model,
                    contentDescription = model.commercialName,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Model Name (device_name_tv, FontMedium.16sp)
            Text(
                text = model.commercialName,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = XiaomiTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Edition / Brand Subtitle (device_edition_tv, FontRegular.12sp, 40% opacity)
            Text(
                text = model.brand.ifBlank { "Xiaomi" },
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = XiaomiTextMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 1:1 Reproduction of device_item_device_list.xml for discovered nearby devices.
 */
@Composable
private fun DiscoveredDeviceItemCard(
    device: DiscoveredDevice,
    onConnectClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(XiaomiCardBg)
            .border(1.dp, XiaomiCardBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onConnectClicked)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val model = remember(device.name) {
            if (device.isXiaomiEarbuds) com.alan.ximiearbuds.core.device.DeviceRegistry.findByName(device.name) else null
        }
        val effectiveModel = if (model != null && model.codename != "GENERIC") model else null

        // Bluetooth / Device Icon Box
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF142436)),
            contentAlignment = Alignment.Center
        ) {
            if (effectiveModel != null) {
                XiaomiDeviceImage(
                    model = effectiveModel,
                    colorType = device.colorType,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = XiaomiCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Device Name & Address
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name.ifBlank { "Xiaomi Earbuds" },
                color = XiaomiTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = device.address,
                color = XiaomiTextMuted,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // "Connecter" Action Button (switch_device_btn)
        Button(
            onClick = onConnectClicked,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = XiaomiCyan,
                contentColor = Color.Black
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = stringRes("device_manager_switch"),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Authentic Bluetooth Radar Pulse Animation (device_item_bluetooth_connect.xml)
 */
@Composable
private fun OfficialRadarScanner(isScanning: Boolean) {
    val transition = rememberInfiniteTransition()

    val wave1 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val wave2 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val alpha1 by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val alpha2 by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isScanning) {
            // Ripple 1
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(wave1)
                    .clip(CircleShape)
                    .border(1.5.dp, XiaomiCyan.copy(alpha = alpha1), CircleShape)
            )
            // Ripple 2
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(wave2)
                    .clip(CircleShape)
                    .border(1.5.dp, XiaomiCyan.copy(alpha = alpha2), CircleShape)
            )
        }

        // Center Pulsing Core
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(XiaomiCyan.copy(alpha = 0.15f))
                .border(1.5.dp, XiaomiCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = XiaomiCyan,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
