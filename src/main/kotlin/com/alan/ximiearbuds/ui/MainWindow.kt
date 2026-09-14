package com.alan.ximiearbuds.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.bluetooth.ConnectionState
import com.alan.ximiearbuds.core.device.DeviceRegistry
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.ui.components.*
import com.alan.ximiearbuds.ui.theme.*

/**
 * Screen navigation states matching official decompiled mobile application:
 * - EMPTY: device_settings_empty_layout.xml (no device connected/paired)
 * - ADD_DEVICE: device_fragment_add_device.xml (radar scan + 2-column small device cards)
 * - SCAN_GUIDE: device_fragment_scan_device.xml (pairing guidance for selected model)
 * - DEVICE_SETTINGS: device_settings_fragment_device_settings.xml (control panel)
 */
enum class AppScreen {
    EMPTY,
    ADD_DEVICE,
    SCAN_GUIDE,
    DEVICE_SETTINGS,
    MORE_SETTINGS
}

/**
 * 1:1 Faithful Reproduction of Xiaomi Earbuds Android App (com.mi.earphone v1.37.1i).
 * - Exact vertical hierarchy from device_settings_fragment_device_settings.xml & device_settings_item_function_layout.xml.
 * - Mobile viewport preservation (avoids stretched desktop dashboard deformation).
 * - Authentic MIUI Action Bar & Dialogs.
 */
@Composable
fun MainWindow(
    controller: EarbudsController,
    isDarkTheme: Boolean,
    currentLanguage: AppLanguage,
    onThemeToggled: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    val connectionState by controller.connectionState.collectAsState()
    val deviceInfo by controller.deviceInfo.collectAsState()
    val activeModel by controller.activeModel.collectAsState()
    val noiseControl by controller.noiseControl.collectAsState()
    val equalizer by controller.equalizer.collectAsState()
    val gestures by controller.gestures.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.EMPTY) }
    var selectedModelForGuide by remember { mutableStateOf<EarbudsModel?>(null) }

    var showSoundEffectDialog by remember { mutableStateOf(false) }
    var showGestureDialog by remember { mutableStateOf(false) }
    var showFindDialog by remember { mutableStateOf(false) }
    var showFitDetectionDialog by remember { mutableStateOf(false) }
    var showEarboxSoundDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showFirmwareDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var showLangMenu by remember { mutableStateOf(false) }

    val isConnected = connectionState == ConnectionState.CONNECTED

    // Automatically transition to DEVICE_SETTINGS when an earbud is connected or active
    LaunchedEffect(isConnected, activeModel) {
        if (currentScreen == AppScreen.EMPTY && (isConnected || activeModel != null)) {
            currentScreen = AppScreen.DEVICE_SETTINGS
        } else if (!isConnected && activeModel == null && (currentScreen == AppScreen.DEVICE_SETTINGS || currentScreen == AppScreen.MORE_SETTINGS)) {
            currentScreen = AppScreen.EMPTY
        }
    }

    val currentModel = activeModel ?: DeviceRegistry.ALL_MODELS.find { it.hasAnc } ?: DeviceRegistry.ALL_MODELS.firstOrNull() ?: DeviceRegistry.GENERIC_MODEL

    val activeDeviceName = if (isConnected && deviceInfo.name.isNotBlank()) {
        deviceInfo.name
    } else {
        activeModel?.commercialName ?: currentModel.commercialName
    }

    // Mobile Viewport Wrapper: Centers the mobile app frame on desktop screens
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF0C0C0E) else Color(0xFFE8E9EC)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .background(XiaomiPageBg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // -------------------------------------------------------------------------
                // 1. Screen Router (Empty -> Add Device -> Scan Guide -> Device Settings)
                // -------------------------------------------------------------------------
                when (currentScreen) {
                    AppScreen.EMPTY -> {
                        // Top Bar for Empty State
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(XiaomiCardBg)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringRes("app_name"),
                                color = XiaomiTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Quick settings in corner: Language & Theme
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showLangMenu = true },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Language",
                                        tint = XiaomiTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onThemeToggled,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Theme",
                                        tint = XiaomiTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        XiaomiEmptyStateView(
                            onAddDeviceClicked = { currentScreen = AppScreen.ADD_DEVICE }
                        )
                    }

                    AppScreen.ADD_DEVICE -> {
                        XiaomiAddDeviceView(
                            controller = controller,
                            onBackClicked = {
                                currentScreen = if (activeModel != null || isConnected) AppScreen.DEVICE_SETTINGS else AppScreen.EMPTY
                            },
                            onModelSelected = { model ->
                                selectedModelForGuide = model
                                currentScreen = AppScreen.SCAN_GUIDE
                            },
                            onDeviceSelected = {
                                currentScreen = AppScreen.DEVICE_SETTINGS
                            }
                        )
                    }

                    AppScreen.SCAN_GUIDE -> {
                        selectedModelForGuide?.let { model ->
                            XiaomiScanDeviceView(
                                model = model,
                                controller = controller,
                                onBackClicked = { currentScreen = AppScreen.ADD_DEVICE }
                            )
                        } ?: run {
                            currentScreen = AppScreen.ADD_DEVICE
                        }
                    }

                    AppScreen.DEVICE_SETTINGS -> {
                        // Authentic MIUI Top Action Bar (device_settings_fragment_device_settings.xml)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(XiaomiCardBg)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Switch / Back to Device List
                            IconButton(
                                onClick = { currentScreen = AppScreen.ADD_DEVICE },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = XiaomiTextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Center: Device Title
                            Text(
                                text = activeDeviceName,
                                color = XiaomiTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                            )

                            // Right: Add (+) & More Settings (...)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { currentScreen = AppScreen.ADD_DEVICE },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringRes("device_add_title"),
                                        tint = XiaomiTextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { currentScreen = AppScreen.MORE_SETTINGS },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringRes("device_settings_more_settings"),
                                        tint = XiaomiTextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Scrollable Body: Replicates NestedScrollView in device_settings_fragment_device_settings.xml
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 1. Device Info Hero & Connection State (device_settings_item_main_device_info)
                            XiaomiHeroBanner(
                                model = activeModel ?: currentModel,
                                deviceName = activeDeviceName,
                                isConnected = isConnected,
                                colorType = deviceInfo.colorType,
                                onColorSelected = { colorId -> controller.setDeviceColor(colorId) }
                            )

                            // 2. Battery Info Container (com.mi.earphone.settings.ui.battery.BatteryInfoContainer)
                            XiaomiBatteryCapsule(
                                deviceInfo = deviceInfo
                            )

                            // 3. Noise Reduction Card (device_settings_item_noise_reduction / NoiseReductionView)
                            if (currentModel.hasAnc || currentModel.hasTransparency) {
                                XiaomiNoiseControlCard(
                                    noiseState = noiseControl,
                                    model = currentModel,
                                    onModeChange = { controller.setNoiseMode(it) },
                                    onAncLevelIndexChange = { idx, raw -> controller.setAncLevelByIndex(idx, raw) },
                                    onTransparencyLevelIndexChange = { idx, raw -> controller.setTransparencyLevelByIndex(idx, raw) },
                                    onAutoNoiseChange = { controller.setAutoNoise(it) },
                                    onSmartDenoiseChange = { controller.setSmartDenoise(it) },
                                    onPersonalizedAncChange = { controller.setPersonalizedNoiseReduction(it) }
                                )
                            }

                            // 4. Function Group 1: Audio & Controls (function_layout1 in device_settings_item_function_layout.xml)
                            XiaomiCardContainer {
                                // Audio Recording / Transcription (function_record)
                                if (currentModel.codename.contains("N75", ignoreCase = true) || currentModel.codename.contains("O70C", ignoreCase = true)) {
                                    XiaomiActionItem(
                                        title = stringRes("device_settings_record_title"),
                                        iconRes = "drawable/device_settings_audio_record.png",
                                        onClick = {}
                                    )
                                    XiaomiItemDivider()
                                }

                                // Translation (function_translate)
                                if (currentModel.codename.contains("O71", ignoreCase = true) || currentModel.codename.contains("O74", ignoreCase = true) || currentModel.codename.contains("O70C", ignoreCase = true)) {
                                    XiaomiActionItem(
                                        title = stringRes("device_settings_translate_title"),
                                        iconRes = "drawable/device_settings_translate.png",
                                        onClick = {}
                                    )
                                    XiaomiItemDivider()
                                }

                                // XiaoAI Voice Assistant (function_super_aivs)
                                XiaomiActionItem(
                                    title = stringRes("device_settings_super_aivs"),
                                    iconRes = "drawable/device_settings_aivs.png",
                                    onClick = {}
                                )
                                XiaomiItemDivider()

                                // Gesture operations (function_gesture)
                                XiaomiActionItem(
                                    title = stringRes("device_settings_gesture_operation"),
                                    iconRes = "drawable/device_settings_ic_gesture.webp",
                                    onClick = { showGestureDialog = true }
                                )
                                XiaomiItemDivider()

                                // Sound settings / EQ (function_sound)
                                XiaomiActionItem(
                                    title = stringRes("device_settings_sound_settings"),
                                    subtitle = stringRes(equalizer.preset.stringKey),
                                    iconRes = "drawable/device_settings_ic_sound_settings.webp",
                                    onClick = { showSoundEffectDialog = true }
                                )
                                XiaomiItemDivider()

                                // Laboratory / Beta features (function_laboratory)
                                XiaomiActionItem(
                                    title = stringRes("device_settings_laboratory_function_title"),
                                    iconRes = "drawable/device_settings_laboratory_function.png",
                                    onClick = { showFitDetectionDialog = true }
                                )
                                XiaomiItemDivider()

                                // More Settings (function_more_setting)
                                XiaomiActionItem(
                                    title = stringRes("device_settings_more_settings"),
                                    iconRes = "drawable/device_settings_ic_more_settings.webp",
                                    onClick = { currentScreen = AppScreen.MORE_SETTINGS }
                                )
                            }

                            // 5. Function Group 2: Device Management (function_layout2)
                            XiaomiCardContainer {
                                // Find Device
                                if (currentModel.hasFindDevice) {
                                    XiaomiActionItem(
                                        title = stringRes("device_settings_find_device"),
                                        iconRes = "drawable/device_settings_ic_find_device.webp",
                                        onClick = { showFindDialog = true }
                                    )
                                    XiaomiItemDivider()
                                }

                                // Firmware Update
                                XiaomiActionItem(
                                    title = stringRes("device_settings_firmware_update"),
                                    badgeText = deviceInfo.versionName.ifBlank { "1.0.8.2" },
                                    iconRes = "drawable/device_settings_ic_firmware_update.webp",
                                    onClick = { showFirmwareDialog = true }
                                )
                            }

                            // 6. Function Group 3: Sports (function_sport_layout)
                            if (currentModel.codename == "O73" || currentModel.isBoneConduction) {
                                XiaomiCardContainer {
                                    XiaomiActionItem(
                                        title = stringRes("device_settings_sport_config"),
                                        iconRes = "drawable/device_settings_sport_settings.png",
                                        onClick = {}
                                    )
                                    XiaomiItemDivider()
                                    XiaomiActionItem(
                                        title = stringRes("device_settings_exercise_report"),
                                        iconRes = "drawable/device_settings_ic_exercise.png",
                                        onClick = {}
                                    )
                                }
                            }

                            // 7. Function Group 4: Help & About
                            XiaomiCardContainer {
                                // Beginner Guide
                                XiaomiActionItem(
                                    title = stringRes("device_settings_beginner_guide"),
                                    iconRes = "drawable/device_settings_function_guide.png",
                                    onClick = { showGuideDialog = true }
                                )
                                XiaomiItemDivider()

                                // FAQ
                                XiaomiActionItem(
                                    title = stringRes("device_settings_questions_answers"),
                                    iconRes = "drawable/device_settings_ic_faq.webp",
                                    onClick = { showFaqDialog = true }
                                )
                                XiaomiItemDivider()

                                // Anti-disconnect protection
                                XiaomiActionItem(
                                    title = stringRes("device_settings_anti_disconnect_protection"),
                                    iconRes = "drawable/device_settings_ic_disconnect_protect.webp",
                                    onClick = {}
                                )
                                XiaomiItemDivider()

                                // About Device
                                XiaomiActionItem(
                                    title = stringRes("device_settings_about_device"),
                                    iconRes = "drawable/device_settings_ic_about_device.webp",
                                    onClick = { showAboutDialog = true }
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }

                    AppScreen.MORE_SETTINGS -> {
                        XiaomiMoreSettingsView(
                            controller = controller,
                            activeModel = activeModel ?: currentModel,
                            onBackClicked = { currentScreen = AppScreen.DEVICE_SETTINGS },
                            onOpenFitDetection = { showFitDetectionDialog = true },
                            onOpenEarboxSound = { showEarboxSoundDialog = true }
                        )
                    }
                }
            }
        }

        // Language Selection Dropdown Menu
        DropdownMenu(
            expanded = showLangMenu,
            onDismissRequest = { showLangMenu = false },
            modifier = Modifier
                .heightIn(max = 420.dp)
                .background(XiaomiCardHover)
        ) {
            for (lang in AppLanguage.entries) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = lang.displayName,
                            color = if (lang == currentLanguage) XiaomiCyan else XiaomiTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (lang == currentLanguage) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onLanguageSelected(lang)
                        showLangMenu = false
                    }
                )
            }
        }
    }

    // -----------------------------------------------------------------------------
    // Official Dialogs & Fragments
    // -----------------------------------------------------------------------------
    if (showSoundEffectDialog) {
        XiaomiSoundEffectDialog(
            controller = controller,
            activeModel = activeModel ?: currentModel,
            onDismiss = { showSoundEffectDialog = false }
        )
    }

    if (showGestureDialog) {
        XiaomiGestureDialog(
            gestureSettings = gestures,
            activeModel = activeModel ?: currentModel,
            onUpdateGestures = { controller.updateGestures(it) },
            onDismiss = { showGestureDialog = false }
        )
    }


    if (showFindDialog) {
        XiaomiFindDeviceDialog(
            onRingLeft = { controller.ringEarbuds(com.alan.ximiearbuds.core.protocol.RingTarget.LEFT) },
            onRingRight = { controller.ringEarbuds(com.alan.ximiearbuds.core.protocol.RingTarget.RIGHT) },
            onDismiss = { showFindDialog = false }
        )
    }

    if (showFitDetectionDialog) {
        XiaomiFitDetectionDialog(
            onDismiss = { showFitDetectionDialog = false }
        )
    }

    if (showEarboxSoundDialog) {
        XiaomiEarboxSoundDialog(
            onDismiss = { showEarboxSoundDialog = false }
        )
    }

    if (showAboutDialog) {
        XiaomiDeviceAboutDialog(
            deviceInfo = deviceInfo,
            activeModel = activeModel ?: currentModel,
            onDismiss = { showAboutDialog = false }
        )
    }

    if (showFirmwareDialog) {
        AlertDialog(
            onDismissRequest = { showFirmwareDialog = false },
            title = {
                Text(
                    text = stringRes("device_settings_firmware_update"),
                    fontWeight = FontWeight.Bold,
                    color = XiaomiTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Version actuelle : ${deviceInfo.versionName.ifBlank { "1.0.8.2" }}",
                        color = XiaomiTextPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Le micrologiciel de vos écouteurs est à jour.",
                        color = XiaomiTextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFirmwareDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = XiaomiCyan)
                ) {
                    Text(stringRes("common_confirm"), color = Color.Black)
                }
            }
        )
    }

    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = {
                Text(
                    text = stringRes("device_settings_questions_answers"),
                    fontWeight = FontWeight.Bold,
                    color = XiaomiTextPrimary
                )
            },
            text = {
                Text(
                    text = "Consultez le guide officiel en ligne pour toutes les questions de connexion et fonctionnement.",
                    color = XiaomiTextSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showFaqDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = XiaomiCyan)
                ) {
                    Text(stringRes("common_confirm"), color = Color.Black)
                }
            }
        )
    }

    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            title = {
                Text(
                    text = stringRes("device_settings_beginner_guide"),
                    fontWeight = FontWeight.Bold,
                    color = XiaomiTextPrimary
                )
            },
            text = {
                Text(
                    text = "Guide d'utilisation Xiaomi Earbuds :\n- Pour appairer, ouvrez le boîtier et appuyez 3s sur le bouton.\n- Contrôlez la réduction de bruit directement depuis les boutons dédiés ci-dessus.",
                    color = XiaomiTextSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showGuideDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = XiaomiCyan)
                ) {
                    Text(stringRes("common_confirm"), color = Color.Black)
                }
            }
        )
    }
}
