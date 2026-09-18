package com.alan.ximiearbuds.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.alan.ximiearbuds.core.device.DevicePreferences
import com.alan.ximiearbuds.core.device.DeviceRegistry
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.core.protocol.OfficialGroupIds
import com.alan.ximiearbuds.core.protocol.OfficialFunctionIds
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
    val otaState by controller.otaState.collectAsState()
    val immerseMode by controller.commutingImmerseMode.collectAsState()

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

                                MiuixUserAvatar(
                                    size = 22.dp,
                                    onClick = {
                                        navStack.clear()
                                        navStack.add(ScreenDestination.Profile)
                                        currentScreen = AppScreen.DEVICE_SETTINGS
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
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
                                // Authentic Action Bar Header (device_settings_layout_setting_header.xml)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // device_name_tv: 24sp, style="@style/FontNormal" (MiSans Normal 24sp)
                                        Text(
                                            text = activeDeviceName,
                                            color = XiaomiTextPrimary,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Normal,
                                            fontFamily = rememberMiSans(),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        // show_all_device_tv: 14sp, FontRegular, text_color_70, drawableEnd device_settings_drawable_end_all_devices
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { navStack.add(ScreenDestination.MyDevices) }
                                        ) {
                                            Text(
                                                text = stringRes("device_settings_show_all_device"),
                                                color = if (isDarkTheme) XiaomiTextSecondary else XiaomiLightTextSecondary,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Image(
                                                painter = painterResource(
                                                    if (isDarkTheme) "drawable/device_settings_drawable_end_all_devices_night.png"
                                                    else "drawable/device_settings_drawable_end_all_devices.png"
                                                ),
                                                contentDescription = null,
                                                modifier = Modifier.height(10.dp)
                                            )
                                        }
                                    }

                                    // Right action buttons (aligned with device_name_tv in device_settings_layout_setting_header.xml)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // add_device_iv: @drawable/device_setting_add_device (wrap_content = 100px/3 = 33.3dp, inner visible + is 21.3dp)
                                        Image(
                                            painter = painterResource(
                                                if (isDarkTheme) "drawable/device_setting_add_device_night.webp"
                                                else "drawable/device_setting_add_device.webp"
                                            ),
                                            contentDescription = stringRes("device_add_title"),
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .clickable { currentScreen = AppScreen.ADD_DEVICE }
                                        )

                                        Spacer(modifier = Modifier.width(20.dp))

                                        // user_avatar_iv: @drawable/avatar_default, 22dp circle
                                        MiuixUserAvatar(
                                            size = 22.dp,
                                            onClick = { navStack.add(ScreenDestination.Profile) }
                                        )
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
                                    if (currentModel.hasGroup(OfficialGroupIds.GROUP_NOISE) || currentModel.hasAnc || currentModel.hasTransparency) {
                                        MiuixNoiseReductionView(
                                            noiseState = noiseControl,
                                            capabilities = currentModel.ancCapabilities,
                                            onModeChange = { controller.setNoiseMode(it) },
                                            onAncLevelChange = { controller.setAncRawLevel(it) },
                                            onTransparencyLevelChange = { controller.setTransparencyRawLevel(it) },
                                            onAdaptiveAncChange = { controller.setAutoNoise(it) },
                                            onSmartDenoiseChange = { controller.setSmartDenoise(it) },
                                            onPersonalizedAncChange = { controller.setPersonalizedNoiseReduction(it) }
                                        )
                                    }

                                    // 3b. Commuting immerse (function_commuting_immerse - 1009).
                                    // 1:1 : carte TwoLine sans icône, visible ssi hasFunction(1009),
                                    // sous-titre = mode courant, clic -> popup single-choice ancré.
                                    if (currentModel.hasFunction(OfficialFunctionIds.FUNC_NOISE_IMMERSE)) {
                                        var immerseMenuOpen by remember { mutableStateOf(false) }
                                        val immerseOptions = listOf(
                                            0 to stringRes("device_settings_commuting_immerse_item_close_mode"),
                                            1 to stringRes("device_settings_commuting_immerse_item_flight_mode"),
                                            2 to stringRes("device_settings_commuting_immerse_item_subway_mode"),
                                            3 to stringRes("device_settings_commuting_immerse_item_HSR_mode")
                                        )
                                        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                                            XiaomiCardContainer {
                                                XiaomiActionItem(
                                                    title = stringRes("device_settings_commuting_immerse"),
                                                    subtitle = immerseOptions.firstOrNull { it.first == immerseMode }?.second
                                                        ?: stringRes("device_settings_commuting_immerse_item_close_mode"),
                                                    onClick = { immerseMenuOpen = true }
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = immerseMenuOpen,
                                                onDismissRequest = { immerseMenuOpen = false }
                                            ) {
                                                immerseOptions.forEach { (mode, label) ->
                                                    DropdownMenuItem(
                                                        text = { Text(label) },
                                                        trailingIcon = if (mode == immerseMode) {
                                                            {
                                                                Image(
                                                                    painter = painterResource("drawable/icon_checked.png"),
                                                                    contentDescription = null
                                                                )
                                                            }
                                                        } else null,
                                                        onClick = {
                                                            immerseMenuOpen = false
                                                            controller.setCommutingImmerse(mode)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 4. Function Group 1: Audio & Controls (function_layout1).
                                    // 1:1 : AUCUN divider entre items (le layout officiel n'en a pas).
                                    XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                                        // Audio Recording / Transcription (function_record - 5003)
                                        if (currentModel.hasFunction(OfficialFunctionIds.FUNC_DEVICE_RECORD)) {
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_record_title"),
                                                iconRes = "drawable/device_settings_audio_record.png",
                                                onClick = { navStack.add(ScreenDestination.VoiceTranslation) }
                                            )
                                        }

                                        // Translation (function_translate - 5008)
                                        if (currentModel.hasFunction(OfficialFunctionIds.FUNC_DEVICE_TRANSLATE)) {
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_translate_title"),
                                                iconRes = "drawable/device_settings_translate.png",
                                                onClick = { navStack.add(ScreenDestination.VoiceTranslation) }
                                            )
                                        }

                                        // XiaoAI Voice Assistant (function_super_aivs - 5009 or voice control 3005)
                                        if (currentModel.hasFunction(OfficialFunctionIds.SUPER_AI) || currentModel.moreSettingsCapabilities.hasVoiceControl) {
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_super_aivs"),
                                                iconRes = "drawable/device_settings_aivs.png",
                                                onClick = { navStack.add(ScreenDestination.XiaoAiSettings) }
                                            )
                                        }

                                        // Gesture operations (function_gesture - Group 4) -> Opens full MiuixGestureScreen
                                        if (currentModel.hasGroup(OfficialGroupIds.GROUP_GESTURE_SETTING) || currentModel.hasGestures) {
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_gesture_operation"),
                                                iconRes = "drawable/device_settings_ic_gesture.webp",
                                                onClick = { navStack.add(ScreenDestination.GestureControl) }
                                            )
                                        }

                                        // Sound settings / EQ (function_sound - Group 2) -> Opens full MiuixSoundEffectsScreen
                                        if (currentModel.hasGroup(OfficialGroupIds.GROUP_VOICE) || currentModel.has10BandEq) {
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_sound_settings"),
                                                subtitle = stringRes(equalizer.preset.stringKey),
                                                iconRes = "drawable/device_settings_ic_sound_settings.webp",
                                                onClick = { navStack.add(ScreenDestination.SoundEffects) }
                                            )
                                        }

                                        // Laboratory (function_laboratory - Group 6) -> Opens full MiuixLaboratoryScreen
                                        if (currentModel.hasGroup(OfficialGroupIds.GROUP_LABORATORY) || currentModel.hasFitDetection) {
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_laboratory_function_title"),
                                                iconRes = "drawable/device_settings_laboratory_function.png",
                                                onClick = { navStack.add(ScreenDestination.Laboratory) }
                                            )
                                        }

                                        // More Settings (function_more_setting - Group 3) -> Opens full MiuixMoreSettingsScreen
                                        if (currentModel.hasGroup(OfficialGroupIds.GROUP_FUNCTION_SETTING)) {
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_more_settings"),
                                                iconRes = "drawable/device_settings_ic_more_settings.webp",
                                                onClick = { navStack.add(ScreenDestination.MoreSettings) }
                                            )
                                        }
                                    }

                                    // 5. Function Group 2: Device Management (function_layout2).
                                    // 1:1 : pastille remind (7dp) sur firmware si MAJ dispo, pas de badge version.
                                    XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                                        // Find Device (function 5001) -> Opens full MiuixFindDeviceScreen
                                        if (currentModel.hasFunction(OfficialFunctionIds.FUNC_FIND_DEVICE) || currentModel.hasFindDevice) {
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_find_device"),
                                                iconRes = "drawable/device_settings_ic_find_device.webp",
                                                onClick = { navStack.add(ScreenDestination.FindDevice) }
                                            )
                                        }

                                        // Firmware Update -> Opens full MiuixFirmwareUpdateScreen
                                        XiaomiActionItem(
                                            title = stringRes("device_settings_firmware_update"),
                                            showRemindDot = !otaState.isLatest,
                                            iconRes = "drawable/device_settings_ic_firmware_update.webp",
                                            onClick = { navStack.add(ScreenDestination.FirmwareUpdate) }
                                        )
                                    }

                                    // 6. Function Group 3: Sports (function_sport_layout - Group 8)
                                    if (currentModel.hasGroup(OfficialGroupIds.GROUP_SPORT) || currentModel.moreSettingsCapabilities.hasSport || currentModel.isBoneConduction) {
                                        XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_sport_config"),
                                                iconRes = "drawable/device_settings_sport_settings.png",
                                                onClick = { navStack.add(ScreenDestination.SportSettings) }
                                            )
                                            if (currentModel.hasFunction(OfficialFunctionIds.FUNC_EXERCISE_REPORT)) {
                                                XiaomiActionItem(
                                                    title = stringRes("device_settings_exercise_report"),
                                                    iconRes = "drawable/device_settings_ic_exercise.png",
                                                    onClick = { navStack.add(ScreenDestination.SportSettings) }
                                                )
                                            }
                                        }
                                    }

                                    // Function Group: Dongle (function_dongle_layout - Group 7).
                                    // 1:1 : icône = device_settings_dongle_settings_icon (via usb_settings_bg).
                                    if (currentModel.hasGroup(OfficialGroupIds.GROUP_USB) || currentModel.hasDongle) {
                                        XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_dongle_settings"),
                                                iconRes = "drawable/device_settings_dongle_settings_icon.png",
                                                onClick = { navStack.add(ScreenDestination.DongleSettings) }
                                            )
                                            XiaomiActionItem(
                                                title = stringRes("device_settings_usb_firmware_update"),
                                                iconRes = "drawable/device_settings_usb_update.webp",
                                                onClick = { navStack.add(ScreenDestination.DongleSettings) }
                                            )
                                        }
                                    }

                                    // Function Group: Help & About (matching device_settings_item_function_layout.xml).
                                    // 1:1 : AUCUN divider entre items.
                                    XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                                        XiaomiActionItem(
                                            title = stringRes("device_settings_beginner_guide"),
                                            iconRes = "drawable/device_settings_function_guide.png",
                                            onClick = { currentScreen = AppScreen.WELCOME }
                                        )
                                        XiaomiActionItem(
                                            title = stringRes("device_settings_device_introduce"),
                                            iconRes = "drawable/device_settings_introduce.webp",
                                            onClick = { navStack.add(ScreenDestination.DeviceInfo) }
                                        )
                                        XiaomiActionItem(
                                            title = stringRes("device_settings_questions_answers"),
                                            iconRes = "drawable/device_settings_ic_faq.webp",
                                            onClick = { navStack.add(ScreenDestination.MoreSettings) }
                                        )
                                        XiaomiActionItem(
                                            title = stringRes("device_settings_anti_disconnect_protection"),
                                            iconRes = "drawable/device_settings_ic_disconnect_protect.webp",
                                            onClick = { navStack.add(ScreenDestination.MoreSettings) }
                                        )
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
                                    controller = controller,
                                    onBackClick = { navStack.removeLast() }
                                )
                            }

                            ScreenDestination.EarboxSound -> {
                                MiuixEarboxSoundScreen(
                                    controller = controller,
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

                            ScreenDestination.Profile -> {
                                MiuixProfileScreen(
                                    onBackClick = {
                                        if (navStack.size > 1) {
                                            navStack.removeLast()
                                        } else {
                                            navStack.clear()
                                            navStack.add(ScreenDestination.MainSettings)
                                            if (!isConnected && activeModel == null) {
                                                currentScreen = AppScreen.EMPTY
                                            }
                                        }
                                    },
                                    onNavigateToSecurityCode = { navStack.add(ScreenDestination.SecurityCode) },
                                    onNavigateToLogin = { navStack.add(ScreenDestination.Login) },
                                    currentLanguage = currentLanguage,
                                    onLanguageSelected = onLanguageSelected
                                )
                            }

                            ScreenDestination.Login -> {
                                MiuixLoginScreen(
                                    onBackClick = {
                                        if (navStack.size > 1) {
                                            navStack.removeLast()
                                        } else {
                                            navStack.clear()
                                            navStack.add(ScreenDestination.Profile)
                                        }
                                    },
                                    onLoginSuccess = { _, _ ->
                                        if (navStack.size > 1) {
                                            navStack.removeLast()
                                        } else {
                                            navStack.clear()
                                            navStack.add(ScreenDestination.Profile)
                                        }
                                    }
                                )
                            }

                            ScreenDestination.SecurityCode -> {
                                MiuixSecurityCodeScreen(
                                    onBackClick = { navStack.removeLast() }
                                )
                            }

                            ScreenDestination.MyDevices -> {
                                MiuixMyDevicesScreen(
                                    controller = controller,
                                    onBackClick = { navStack.removeLast() },
                                    onNavigateToAddDevice = { currentScreen = AppScreen.ADD_DEVICE },
                                    onDeviceSelected = {
                                        navStack.removeLast()
                                    }
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
