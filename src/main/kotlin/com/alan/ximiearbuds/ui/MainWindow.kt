package com.alan.ximiearbuds.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.bluetooth.ConnectionState
import com.alan.ximiearbuds.core.device.DevicePreferences
import com.alan.ximiearbuds.core.device.DeviceRegistry
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.ui.components.*
import com.alan.ximiearbuds.ui.navigation.ScreenDestination
import com.alan.ximiearbuds.ui.screens.*
import com.alan.ximiearbuds.ui.theme.*

/**
 * Screen navigation states matching official decompiled mobile application:
 * - WELCOME: login_activity_guide.xml (GuideActivity onboarding carousel)
 * - EMPTY: device_settings_empty_layout.xml (no device connected/paired)
 * - ADD_DEVICE: device_fragment_add_device.xml (radar scan + 2-column small device cards)
 * - SCAN_GUIDE: device_fragment_scan_device.xml (pairing guidance for selected model)
 * - DEVICE_SETTINGS: device_settings_fragment_device_settings.xml (control panel)
 */
enum class AppScreen {
    WELCOME,
    EMPTY,
    ADD_DEVICE,
    SCAN_GUIDE,
    DEVICE_SETTINGS
}

/**
 * 1:1 Faithful Reproduction of Xiaomi Earbuds Android App (com.mi.earphone v1.37.1i).
 * - Exact vertical hierarchy from device_settings_fragment_device_settings.xml & device_settings_item_function_layout.xml.
 * - Mobile viewport preservation (avoids stretched desktop dashboard deformation).
 * - Authentic MIUI Action Bar & Full-Screen Navigation Stack (replacing makeshift popups).
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

    val isConnected = connectionState == ConnectionState.CONNECTED
    val isWelcomeFinished = remember { DevicePreferences.isWelcomeFinished() }
    var currentScreen by remember {
        mutableStateOf(
            if (!isWelcomeFinished && activeModel == null && !isConnected) {
                AppScreen.WELCOME
            } else if (activeModel != null || isConnected) {
                AppScreen.DEVICE_SETTINGS
            } else {
                AppScreen.EMPTY
            }
        )
    }
    var selectedModelForGuide by remember { mutableStateOf<EarbudsModel?>(null) }
    var showLangMenu by remember { mutableStateOf(false) }

    // Screen backstack within the connected device settings viewport
    val navStack = remember { mutableStateListOf<ScreenDestination>(ScreenDestination.MainSettings) }
    val currentDestination = navStack.lastOrNull() ?: ScreenDestination.MainSettings

    // Automatically transition to DEVICE_SETTINGS when an earbud is connected or active
    LaunchedEffect(isConnected, activeModel) {
        if (currentScreen == AppScreen.EMPTY && (isConnected || activeModel != null)) {
            currentScreen = AppScreen.DEVICE_SETTINGS
        } else if (!isConnected && activeModel == null && currentScreen == AppScreen.DEVICE_SETTINGS) {
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
                // 1. Root Screen Router (Welcome -> Empty -> Add Device -> Scan Guide -> Settings)
                // -------------------------------------------------------------------------
                when (currentScreen) {
                    AppScreen.WELCOME -> {
                        MiuixWelcomeGuideScreen(
                            onFinish = {
                                currentScreen = if (activeModel != null || isConnected) AppScreen.DEVICE_SETTINGS else AppScreen.EMPTY
                            }
                        )
                    }

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

                            // Quick settings in corner: Guide, Language & Theme
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { currentScreen = AppScreen.WELCOME },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = "Welcome Guide",
                                        tint = XiaomiTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

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

                        // Empty State View (device_settings_empty_layout.xml)
                        XiaomiEmptyStateView(
                            onAddDeviceClicked = { currentScreen = AppScreen.ADD_DEVICE }
                        )
                    }

                    AppScreen.ADD_DEVICE -> {
                        XiaomiAddDeviceView(
                            controller = controller,
                            onModelSelected = { model ->
                                selectedModelForGuide = model
                                currentScreen = AppScreen.SCAN_GUIDE
                            },
                            onBackClicked = {
                                currentScreen = if (activeModel != null || isConnected) AppScreen.DEVICE_SETTINGS else AppScreen.EMPTY
                            }
                        )
                    }

                    AppScreen.SCAN_GUIDE -> {
                        val model = selectedModelForGuide ?: currentModel
                        XiaomiScanDeviceView(
                            model = model,
                            controller = controller,
                            onBackClicked = {
                                currentScreen = AppScreen.ADD_DEVICE
                            }
                        )
                    }

                    AppScreen.DEVICE_SETTINGS -> {
                        // -------------------------------------------------------------------------
                        // 2. Sub-Screen Navigation Router (Main -> EQ -> Gestures -> More -> Info)
                        // -------------------------------------------------------------------------
                        when (currentDestination) {
                            ScreenDestination.MainSettings -> {
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
                                            onClick = { navStack.add(ScreenDestination.MoreSettings) },
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
                                        .padding(vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // 1. Device Info Hero & Connection State (device_settings_item_main_device_info)
                                    XiaomiHeroBanner(
                                        model = activeModel ?: currentModel,
                                        deviceName = activeDeviceName,
                                        isConnected = isConnected,
                                        colorType = deviceInfo.colorType,
                                        onColorSelected = { colorId -> controller.setDeviceColor(colorId) },
                                        onSkinClick = { navStack.add(ScreenDestination.PersonalSkin) }
                                    )

                                    // 2. Authentic Battery Container 1:1 (device_settings_layout_battery.xml)
                                    MiuixBatteryContainer(
                                        deviceInfo = deviceInfo
                                    )

                                    // 3. Authentic Noise Reduction Card 1:1 (device_settings_layout_noise_redution.xml)
                                    if (currentModel.hasAnc || currentModel.hasTransparency) {
                                        MiuixNoiseReductionView(
                                            noiseState = noiseControl,
                                            onModeChange = { controller.setNoiseMode(it) },
                                            onAncLevelChange = { controller.setAncLevel(it) },
                                            onTransparencyLevelChange = { controller.setTransparencyLevel(it) },
                                            onSmartDenoiseChange = { controller.setSmartDenoise(it) },
                                            onPersonalizedAncChange = { controller.setPersonalizedNoiseReduction(it) }
                                        )
                                    }

                                    // 4. Function Group 1: Audio & Controls (function_layout1)
                                    XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                                        // Audio Recording / Transcription (function_record)
                                        if (currentModel.codename.contains("N75", ignoreCase = true) || currentModel.codename.contains("O70C", ignoreCase = true)) {
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_record_title"),
                                                iconRes = "drawable/device_settings_audio_record.png",
                                                onClick = { navStack.add(ScreenDestination.VoiceTranslation) }
                                            )
                                            XiaomiItemDivider()
                                        }

                                        // Translation (function_translate)
                                        if (currentModel.codename.contains("O71", ignoreCase = true) || currentModel.codename.contains("O74", ignoreCase = true) || currentModel.codename.contains("O70C", ignoreCase = true)) {
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_translate_title"),
                                                iconRes = "drawable/device_settings_translate.png",
                                                onClick = { navStack.add(ScreenDestination.VoiceTranslation) }
                                            )
                                            XiaomiItemDivider()
                                        }

                                        // XiaoAI Voice Assistant (function_super_aivs)
                                        XiaomiActionItem(
                                            title = stringRes("device_settings_super_aivs"),
                                            iconRes = "drawable/device_settings_aivs.png",
                                            onClick = { navStack.add(ScreenDestination.XiaoAiSettings) }
                                        )
                                        XiaomiItemDivider()

                                        // Gesture operations (function_gesture) -> Opens full MiuixGestureScreen
                                        XiaomiActionItem(
                                            title = stringRes("device_settings_gesture_operation"),
                                            iconRes = "drawable/device_settings_ic_gesture.webp",
                                            onClick = { navStack.add(ScreenDestination.GestureControl) }
                                        )
                                        XiaomiItemDivider()

                                        // Sound settings / EQ (function_sound) -> Opens full MiuixSoundEffectsScreen
                                        XiaomiActionItem(
                                            title = stringRes("device_settings_sound_settings"),
                                            subtitle = stringRes(equalizer.preset.stringKey),
                                            iconRes = "drawable/device_settings_ic_sound_settings.webp",
                                            onClick = { navStack.add(ScreenDestination.SoundEffects) }
                                        )
                                        XiaomiItemDivider()

                                        // Laboratory / Fit detection -> Opens full MiuixLaboratoryScreen
                                        XiaomiActionItem(
                                            title = stringRes("device_settings_laboratory_function_title"),
                                            iconRes = "drawable/device_settings_laboratory_function.png",
                                            onClick = { navStack.add(ScreenDestination.Laboratory) }
                                        )
                                        XiaomiItemDivider()

                                        // More Settings (function_more_setting) -> Opens full MiuixMoreSettingsScreen
                                        XiaomiActionItem(
                                            title = stringRes("device_settings_more_settings"),
                                            iconRes = "drawable/device_settings_ic_more_settings.webp",
                                            onClick = { navStack.add(ScreenDestination.MoreSettings) }
                                        )
                                    }

                                    // 5. Function Group 2: Device Management (function_layout2)
                                    XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                                        // Find Device -> Opens full MiuixFindDeviceScreen
                                        if (currentModel.hasFindDevice) {
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_find_device"),
                                                iconRes = "drawable/device_settings_ic_find_device.webp",
                                                onClick = { navStack.add(ScreenDestination.FindDevice) }
                                            )
                                            XiaomiItemDivider()
                                        }

                                        // Firmware Update -> Opens full MiuixFirmwareUpdateScreen
                                        XiaomiActionItem(
                                            title = stringRes("device_settings_firmware_update"),
                                            badgeText = deviceInfo.versionName.ifBlank { "1.0.8.2" },
                                            iconRes = "drawable/device_settings_ic_firmware_update.webp",
                                            onClick = { navStack.add(ScreenDestination.FirmwareUpdate) }
                                        )
                                    }

                                    // 6. Function Group 3: Sports (function_sport_layout)
                                    if (currentModel.codename == "O73" || currentModel.isBoneConduction) {
                                        XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_sport_config"),
                                                iconRes = "drawable/device_settings_sport_settings.png",
                                                onClick = { navStack.add(ScreenDestination.SportSettings) }
                                            )
                                            XiaomiItemDivider()
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_exercise_report"),
                                                iconRes = "drawable/device_settings_ic_exercise.png",
                                                onClick = { navStack.add(ScreenDestination.SportSettings) }
                                            )
                                        }
                                    }

                                    // 7. Function Group 4: Help & About
                                    XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                                        // About Device -> Opens full MiuixDeviceInfoScreen
                                        XiaomiActionItem(
                                            title = stringRes("device_settings_about_device"),
                                            iconRes = "drawable/device_settings_ic_about_device.webp",
                                            onClick = { navStack.add(ScreenDestination.DeviceInfo) }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }

                            ScreenDestination.MoreSettings -> {
                                MiuixMoreSettingsScreen(
                                    controller = controller,
                                    onBackClick = { navStack.removeLast() },
                                    onNavigateToEarbox = { navStack.add(ScreenDestination.EarboxSound) },
                                    onNavigateToFitDetection = { navStack.add(ScreenDestination.FitDetection) },
                                    onNavigateToDeviceInfo = { navStack.add(ScreenDestination.DeviceInfo) },
                                    onNavigateToDongle = { navStack.add(ScreenDestination.DongleSettings) }
                                )
                            }

                            ScreenDestination.CustomizedEq -> {
                                MiuixEqualizerScreen(
                                    controller = controller,
                                    onBackClick = { navStack.removeLast() }
                                )
                            }

                            ScreenDestination.SoundEffects -> {
                                MiuixSoundEffectsScreen(
                                    controller = controller,
                                    onBackClick = { navStack.removeLast() },
                                    onNavigateToEqualizer = { navStack.add(ScreenDestination.CustomizedEq) },
                                    onNavigateToSpatialAudio = { navStack.add(ScreenDestination.SpatialAudio) }
                                )
                            }

                            ScreenDestination.GestureControl -> {
                                MiuixGestureScreen(
                                    controller = controller,
                                    onBackClick = { navStack.removeLast() }
                                )
                            }

                            ScreenDestination.FindDevice -> {
                                MiuixFindDeviceScreen(
                                    controller = controller,
                                    onBackClick = { navStack.removeLast() }
                                )
                            }

                            ScreenDestination.FitDetection -> {
                                MiuixFitDetectionScreen(
                                    onBackClick = { navStack.removeLast() }
                                )
                            }

                            ScreenDestination.EarboxSound -> {
                                MiuixEarboxSoundScreen(
                                    onBackClick = { navStack.removeLast() }
                                )
                            }

                            ScreenDestination.DeviceInfo -> {
                                MiuixDeviceInfoScreen(
                                    deviceInfo = deviceInfo,
                                    activeModel = activeModel ?: currentModel,
                                    onBackClick = { navStack.removeLast() },
                                    onNavigateToGuide = { navStack.add(ScreenDestination.BeginnerGuide) }
                                )
                            }

                            ScreenDestination.SpatialAudio -> {
                                MiuixSpatialAudioScreen(
                                    controller = controller,
                                    onBackClick = { navStack.removeLast() }
                                )
                            }

                            ScreenDestination.FirmwareUpdate -> {
                                MiuixFirmwareUpdateScreen(
                                    controller = controller,
                                    activeModel = activeModel ?: currentModel,
                                    onBackClick = { navStack.removeLast() }
                                )
                            }

                            ScreenDestination.DongleSettings -> {
                                MiuixDongleSettingsScreen(
                                    controller = controller,
                                    onBackClick = { navStack.removeLast() }
                                )
                            }

                            ScreenDestination.XiaoAiSettings -> {
                                MiuixXiaoAiScreen(
                                    controller = controller,
                                    onBackClick = { navStack.removeLast() }
                                )
                            }

                            ScreenDestination.Laboratory -> {
                                MiuixLaboratoryScreen(
                                    controller = controller,
                                    onBackClick = { navStack.removeLast() },
                                    onNavigateToFitDetection = { navStack.add(ScreenDestination.FitDetection) }
                                )
                            }

                            ScreenDestination.PersonalSkin -> {
                                MiuixPersonalSkinScreen(
                                    controller = controller,
                                    activeModel = activeModel ?: currentModel,
                                    onBackClick = { navStack.removeLast() }
                                )
                            }

                            ScreenDestination.SportSettings -> {
                                MiuixSportSettingsScreen(
                                    controller = controller,
                                    onBackClick = { navStack.removeLast() }
                                )
                            }

                            ScreenDestination.VoiceTranslation -> {
                                MiuixVoiceTranslationScreen(
                                    controller = controller,
                                    onBackClick = { navStack.removeLast() }
                                )
                            }

                            ScreenDestination.BeginnerGuide -> {
                                MiuixWelcomeGuideScreen(
                                    onFinish = { navStack.removeLast() }
                                )
                            }

                            else -> {
                                // Fallback
                                navStack.clear()
                                navStack.add(ScreenDestination.MainSettings)
                            }
                        }
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
}
