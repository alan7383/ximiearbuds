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
 * 2. If no devices found yet:
 *    Scanning section (device_item_bluetooth_connect.xml) borderless directly on background:
 *    - 144dp x 144dp Electric Blue (#0D84FF) Bluetooth scanning radar matching lottie/scanning.json
 *    - FontRegular 12sp @string/device_bluetooth_connect_hint (40% white)
 * 3. When nearby devices are discovered (AddDeviceViewModel.java lines 375-410):
 *    Radar is replaced by discovered devices displayed in the 2-column grid as
 *    official device_item_small_device_card.xml with @string/device_manager_distance_closest ("Signal le plus fort")
 *    or @string/device_manager_paired ("Appairé") tag.
 * 4. Section divider with @string/device_add_self ("Ajouter des écouteurs manuellement")
 * 5. 2-column grid of official device cards (device_item_small_device_card.xml):
 *    - 167dp height, 20dp corner radius (device_scan_item_bg)
 *    - 96dp x 96dp authentic device render image
 *    - FontMedium 15-16sp title (device_name_tv)
 *    - FontRegular 12sp subtitle (device_edition_tv, 40% text color)
 *
 * NO artificial gray card box around radar, NO cyan "Utiliser" button.
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
                .background(XiaomiPageBg)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = XiaomiTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringRes("device_add_title"),
                color = XiaomiTextPrimary,
                fontSize = 20.sp,
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // A. Section Title: "Recherche des écouteurs à proximité…" (device_searching_btn_text)
            // or "Aucun écouteur trouvé. Essayez de les ajouter manuellement." (device_not_find_to_add_self)
            val searchTitle = if (!isScanning && allVisibleDevices.isEmpty()) {
                stringRes("device_not_find_to_add_self")
            } else {
                stringRes("device_searching_btn_text")
            }

            Text(
                text = searchTitle,
                color = XiaomiTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 12.dp)
            )

            // B. Scanning Radar OR Discovered Devices
            // Replicates AddDeviceViewModel.java lines 375-410:
            // If no devices found yet: show device_item_bluetooth_connect.xml
            // When devices are discovered: replace radar with discovered devices as device_item_small_device_card.xml
            if (allVisibleDevices.isEmpty()) {
                OfficialBluetoothConnectView(isScanning = isScanning)
            } else {
                val chunkedDiscovered = remember(allVisibleDevices) {
                    allVisibleDevices.chunked(2)
                }
                chunkedDiscovered.forEach { rowDevices ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val d1 = rowDevices[0]
                        val m1 = remember(d1.name) {
                            if (d1.isXiaomiEarbuds) DeviceRegistry.findByName(d1.name) else null
                        }
                        val isPaired1 = pairedDevices.any { it.address.equals(d1.address, ignoreCase = true) } || d1.isConnected
                        val tag1 = if (isPaired1) {
                            stringRes("device_manager_paired")
                        } else {
                            stringRes("device_manager_distance_closest")
                        }
                        SmallDeviceCard(
                            commercialName = d1.name.ifBlank { m1?.commercialName ?: "Xiaomi Earbuds" },
                            brandOrEdition = m1?.brand?.ifBlank { "Xiaomi" } ?: "Xiaomi",
                            model = m1,
                            colorType = d1.colorType,
                            tag = tag1,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                controller.connectToDevice(d1)
                                onDeviceSelected?.invoke(d1)
                            }
                        )

                        if (rowDevices.size > 1) {
                            val d2 = rowDevices[1]
                            val m2 = remember(d2.name) {
                                if (d2.isXiaomiEarbuds) DeviceRegistry.findByName(d2.name) else null
                            }
                            val isPaired2 = pairedDevices.any { it.address.equals(d2.address, ignoreCase = true) } || d2.isConnected
                            val tag2 = if (isPaired2) {
                                stringRes("device_manager_paired")
                            } else {
                                null
                            }
                            SmallDeviceCard(
                                commercialName = d2.name.ifBlank { m2?.commercialName ?: "Xiaomi Earbuds" },
                                brandOrEdition = m2?.brand?.ifBlank { "Xiaomi" } ?: "Xiaomi",
                                model = m2,
                                colorType = d2.colorType,
                                tag = tag2,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    controller.connectToDevice(d2)
                                    onDeviceSelected?.invoke(d2)
                                }
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // C. Section Divider with Title: "Ajouter des écouteurs manuellement" (device_add_self)
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

            // D. 2-Column Grid: Official device_item_small_device_card.xml
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
                    SmallDeviceCard(
                        commercialName = rowModels[0].commercialName,
                        brandOrEdition = rowModels[0].brand.ifBlank { "Xiaomi" },
                        model = rowModels[0],
                        modifier = Modifier.weight(1f),
                        onClick = { onModelSelected(rowModels[0]) }
                    )

                    if (rowModels.size > 1) {
                        SmallDeviceCard(
                            commercialName = rowModels[1].commercialName,
                            brandOrEdition = rowModels[1].brand.ifBlank { "Xiaomi" },
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
 * 1:1 Reproduction of official device_item_bluetooth_connect.xml
 * Displays the 144dp x 144dp Electric Blue Bluetooth scanning radar
 * and the centered hint text directly on the page background (no card container).
 */
@Composable
private fun OfficialBluetoothConnectView(
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 30.dp, bottom = 41.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OfficialRadarScanner(isScanning = isScanning)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringRes("device_bluetooth_connect_hint"),
            fontSize = 12.sp,
            color = XiaomiTextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

/**
 * 1:1 Reproduction of device_item_small_device_card.xml
 * ConstraintLayout with height 167dp, device_scan_item_bg (20dp corner radius),
 * device_tag_tv (13.33sp medium, @color/colorPrimary, @drawable/device_bg_small_device_card_tag),
 * device_icon_iv (96dp x 96dp), device_name_tv (16sp medium), and device_edition_tv (12sp regular 40%).
 */
@Composable
private fun SmallDeviceCard(
    commercialName: String,
    brandOrEdition: String,
    model: EarbudsModel?,
    modifier: Modifier = Modifier,
    colorType: Int? = null,
    tag: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(167.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(XiaomiCardBg)
            .border(width = 1.dp, color = XiaomiCardBorder, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Top-right Tag (device_tag_tv)
        if (!tag.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 2.dp)
                    .background(
                        color = XiaomiElectricBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(133.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = tag,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = XiaomiElectricBlue,
                    maxLines = 1
                )
            }
        }

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
                if (model != null && model.codename != "GENERIC") {
                    XiaomiDeviceImage(
                        model = model,
                        colorType = colorType,
                        contentDescription = commercialName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = XiaomiElectricBlue,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Model Name (device_name_tv, FontMedium.16sp)
            Text(
                text = commercialName,
                fontSize = 15.sp,
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
                text = brandOrEdition,
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
 * Authentic Bluetooth Radar Pulse Animation (device_item_bluetooth_connect.xml)
 * 144dp x 144dp Xiaomi HyperOS scanning radar matching lottie/scanning.json
 * with XiaomiElectricBlue (#0D84FF) expanding ripples and white Bluetooth glyph.
 */
@Composable
private fun OfficialRadarScanner(
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition()

    val wave1 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha1 by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val wave2 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val wave3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha3 by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier.size(144.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isScanning) {
            // Ripple 1
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .scale(wave1)
                    .clip(CircleShape)
                    .background(XiaomiElectricBlue.copy(alpha = alpha1))
            )
            // Ripple 2
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .scale(wave2)
                    .clip(CircleShape)
                    .background(XiaomiElectricBlue.copy(alpha = alpha2))
            )
            // Ripple 3
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .scale(wave3)
                    .clip(CircleShape)
                    .background(XiaomiElectricBlue.copy(alpha = alpha3))
            )
        }

        // Center Core: 48dp solid Xiaomi Electric Blue circle (#0D84FF) with white Bluetooth icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(XiaomiElectricBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
