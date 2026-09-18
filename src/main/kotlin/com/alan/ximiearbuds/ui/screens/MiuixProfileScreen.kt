package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.alan.ximiearbuds.core.device.DevicePreferences
import com.alan.ximiearbuds.ui.components.*
import com.alan.ximiearbuds.ui.theme.*
import java.awt.Desktop
import java.net.URI

/**
 * 1:1 Faithful Reproduction of Xiaomi Earbuds `mine_fragment_mine.xml` & `MineFragment.java`.
 * Hosts the user profile, Xiaomi account login/logout, region & permissions,
 * agreements, feedback, experience program and security code query.
 */
@Composable
fun MiuixProfileScreen(
    onBackClick: () -> Unit,
    onNavigateToSecurityCode: () -> Unit,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // State backed by DevicePreferences
    var isLoggedIn by remember { mutableStateOf(DevicePreferences.isLoggedIn()) }
    var userId by remember { mutableStateOf(DevicePreferences.getUserId()) }
    var userName by remember { mutableStateOf(DevicePreferences.getUserName()) }
    var currentRegion by remember { mutableStateOf(DevicePreferences.getRegion()) }
    var userExperienceAccepted by remember { mutableStateOf(DevicePreferences.isUserExperienceAccepted()) }
    var deviceAssociated by remember { mutableStateOf(DevicePreferences.isDeviceAssociated()) }

    // Dialog Visibility States
    var showAccountInfoDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showAgreementDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showRevokeDialog by remember { mutableStateOf(false) }
    var showExperienceInfoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        MiuixTopAppBar(
            title = stringRes("mine_label"),
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(vertical = 12.dp)
        ) {
            // =========================================================================
            // 1. Profile Header Group (profileGroup: avatarView + nickView + idView)
            // =========================================================================
            val profileInteraction = remember { MutableInteractionSource() }
            val profilePressed by profileInteraction.collectIsPressedAsState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (profilePressed) Color.White.copy(alpha = 0.04f) else Color.Transparent)
                    .clickable(
                        interactionSource = profileInteraction,
                        indication = null,
                        onClick = {
                            if (!isLoggedIn) {
                                showLoginDialog = true
                            } else {
                                showAccountInfoDialog = true
                            }
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // avatarView: 60dp circle with real avatar support
                val avatarPath = remember(isLoggedIn) { DevicePreferences.getAvatarAddress() }
                val avatarBitmap = remember(avatarPath) {
                    if (!avatarPath.isNullOrEmpty()) {
                        val file = java.io.File(avatarPath)
                        if (file.exists()) {
                            try {
                                file.inputStream().use { inputStream ->
                                    androidx.compose.ui.res.loadImageBitmap(inputStream)
                                }
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                    } else null
                }

                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), CircleShape)
                    )
                } else {
                    Image(
                        painter = painterResource("drawable/avatar_default.png"),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // nickView: 15sp FontLatinMedium
                    Text(
                        text = if (isLoggedIn) (userName ?: "Compte Xiaomi") else stringRes("mine_click_login"),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // idView: 12sp FontLatinRegular
                    val userIdTemplate = stringRes("mine_user_id")
                    val idEmptyText = stringRes("mine_id_empty")
                    val idText = if (isLoggedIn) {
                        if (userIdTemplate.contains("%s") || userIdTemplate.contains("%1\$s")) {
                            userIdTemplate.replace("%1\$s", userId ?: "8192384729").replace("%s", userId ?: "8192384729")
                        } else {
                            "$userIdTemplate : ${userId ?: "8192384729"}"
                        }
                    } else {
                        idEmptyText
                    }

                    Text(
                        text = idText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }

                Image(
                    painter = painterResource("drawable/ic_base_right_arrow_icon.webp"),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 2. Card 1: Region, Language & Permissions
            // =========================================================================
            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                // areaView: Région
                XiaomiActionItem(
                    title = stringRes("mine_area"),
                    badgeText = currentRegion,
                    onClick = { showRegionDialog = true }
                )

                XiaomiItemDivider(hasIcon = false)

                // Language selection
                XiaomiActionItem(
                    title = stringRes("device_settings_select_language_title"),
                    badgeText = currentLanguage.displayName,
                    onClick = { showLanguageDialog = true }
                )

                XiaomiItemDivider(hasIcon = false)

                // system_permission_management: Gérer les autorisations
                XiaomiActionItem(
                    title = stringRes("mine_system_permission_management"),
                    badgeText = stringRes("mine_permission_granted"),
                    onClick = { showPermissionsDialog = true }
                )

                XiaomiItemDivider(hasIcon = false)

                // device_manager_auth: Autorisation de gestion des appareils
                XiaomiActionItem(
                    title = stringRes("mine_device_auth_title"),
                    badgeText = if (deviceAssociated) stringRes("mine_device_auth_status_yes") else stringRes("mine_device_auth_status_no"),
                    onClick = {
                        deviceAssociated = !deviceAssociated
                        DevicePreferences.setDeviceAssociated(deviceAssociated)
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 3. Card 2: Feedback & Version
            // =========================================================================
            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                // feedback: Commentaires
                XiaomiActionItem(
                    title = stringRes("mine_feedback"),
                    onClick = { showFeedbackDialog = true }
                )

                XiaomiItemDivider(hasIcon = false)

                // curVersion: Version actuelle de l'application
                XiaomiActionItem(
                    title = stringRes("mine_app_current_version"),
                    badgeText = "v1.37.1i",
                    onClick = { showVersionDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 4. Card 3: Legal & Privacy Policies
            // =========================================================================
            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                // schemaView: Accord Utilisateur
                XiaomiActionItem(
                    title = stringRes("mine_user_agreement"),
                    onClick = { showAgreementDialog = true }
                )

                XiaomiItemDivider(hasIcon = false)

                // privacyView: Politique de Confidentialité
                XiaomiActionItem(
                    title = stringRes("mine_user_privacy_policy"),
                    onClick = { showPrivacyDialog = true }
                )

                XiaomiItemDivider(hasIcon = false)

                // revokePrivacyView: Retrait du consentement (Two-line)
                XiaomiActionItem(
                    title = stringRes("mine_revoke_privacy_granted"),
                    subtitle = stringRes("mine_revoke_privacy_granted_des"),
                    onClick = { showRevokeDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // 5. Card 4: Experience Program & Security Code
            // =========================================================================
            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                // user_experience_imporvement: Programme d'Expérience Utilisateur
                XiaomiActionItem(
                    title = stringRes("mine_user_experience_improvement"),
                    onClick = { showExperienceInfoDialog = true }
                )

                XiaomiItemDivider(hasIcon = false)

                // engage_user_experience_improvement: Inscription au Programme
                XiaomiSwitchItem(
                    title = stringRes("mine_user_experience_improvement_engage"),
                    subtitle = stringRes("mine_user_experience_improvement_engage_detail"),
                    checked = userExperienceAccepted,
                    onCheckedChange = { accepted ->
                        userExperienceAccepted = accepted
                        DevicePreferences.setUserExperienceAccepted(accepted)
                    }
                )

                XiaomiItemDivider(hasIcon = false)

                // security_code: Requête de code de sécurité
                XiaomiActionItem(
                    title = stringRes("mine_security_title"),
                    onClick = onNavigateToSecurityCode
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // 6. Logout Button (logoutView)
            // =========================================================================
            if (isLoggedIn) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 27.dp)
                ) {
                    Button(
                        onClick = { showLogoutDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = XiaomiRed
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = stringRes("mine_logout"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // =========================================================================
            // 7. ICP Filing Footer (case_number_tv)
            // =========================================================================
            Text(
                text = stringRes("mine_case_number"),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 27.dp, vertical = 12.dp)
            )
        }
    }

    // =========================================================================
    // DIALOGS
    // =========================================================================

    // 0. Account Info Dialog (Authentic Xiaomi Account Management 1:1)
    if (showAccountInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAccountInfoDialog = false },
            title = {
                Text(
                    text = stringRes("passport_account_secure_info_pref_title"),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${stringRes("passport_account_user_name")} : ${userName ?: stringRes("mine_id_empty")}",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${stringRes("passport_account_user_id")} : ${userId ?: stringRes("passport_account_none_user_name")}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${stringRes("mine_area")} : $currentRegion",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${stringRes("device_settings_device_connected")} • ${stringRes("mine_safe_check")}",
                        fontSize = 13.sp,
                        color = XiaomiGreen
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                Desktop.getDesktop().browse(URI("https://account.xiaomi.com"))
                            } else {
                                Runtime.getRuntime().exec(arrayOf("xdg-open", "https://account.xiaomi.com"))
                            }
                        } catch (ignored: Exception) {}
                        showAccountInfoDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = XiaomiCyan)
                ) {
                    Text(stringRes("passport_account_user_details"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountInfoDialog = false }) {
                    Text(stringRes("close"))
                }
            }
        )
    }

    // 1. Login Dialog (Authentic Xiaomi Passport Login 1:1)
    if (showLoginDialog) {
        MiuixLoginDialog(
            onDismissRequest = { showLoginDialog = false },
            onLoginSuccess = { newUserId, newUserName ->
                isLoggedIn = true
                userId = newUserId
                userName = newUserName
                showLoginDialog = false
            }
        )
    }

    // 2. Logout Confirmation Dialog (mine_logout_exit)
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = stringRes("mine_logout_exit"),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = stringRes("mine_cache_clear"),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        com.alan.ximiearbuds.core.account.XiaomiAccountClient.logout()
                        isLoggedIn = false
                        userId = null
                        userName = null
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = XiaomiRed)
                ) {
                    Text(stringRes("mine_logout_confirm"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringRes("cancel"))
                }
            }
        )
    }

    // 3. Region Selector Dialog
    if (showRegionDialog) {
        val regions = listOf(
            "France", "United States", "Deutschland", "España", "Italia",
            "United Kingdom", "中国 (China)", "台灣 (Taiwan)", "Japan", "South Korea"
        )
        AlertDialog(
            onDismissRequest = { showRegionDialog = false },
            title = { Text(stringRes("mine_area"), fontWeight = FontWeight.SemiBold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    regions.forEach { reg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentRegion = reg
                                    DevicePreferences.setRegion(reg)
                                    showRegionDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = reg, fontSize = 15.sp)
                            if (reg == currentRegion) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = XiaomiCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRegionDialog = false }) {
                    Text(stringRes("cancel"))
                }
            }
        )
    }

    // 4. Language Selector Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringRes("device_settings_select_language_title"), fontWeight = FontWeight.SemiBold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLanguageSelected(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang.displayName,
                                fontSize = 15.sp,
                                fontWeight = if (lang == currentLanguage) FontWeight.Bold else FontWeight.Normal,
                                color = if (lang == currentLanguage) XiaomiCyan else MaterialTheme.colorScheme.onSurface
                            )
                            if (lang == currentLanguage) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = XiaomiCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringRes("cancel"))
                }
            }
        )
    }

    // 5. System Permissions Dialog
    if (showPermissionsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionsDialog = false },
            title = { Text(stringRes("mine_system_permission_management"), fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = XiaomiCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(stringRes("privacy_permission_location"), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(stringRes("mine_permission_granted"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = XiaomiCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(stringRes("privacy_permission_mic_permission"), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(stringRes("mine_permission_granted"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = XiaomiCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(stringRes("privacy_permission_write_read_file"), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(stringRes("mine_permission_granted"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPermissionsDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = XiaomiCyan)) {
                    Text(stringRes("passport_i_know"))
                }
            }
        )
    }

    // 6. Feedback Dialog
    if (showFeedbackDialog) {
        var feedbackText by remember { mutableStateOf("") }
        var attachLogs by remember { mutableStateOf(true) }
        var submitted by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = { Text(stringRes("mine_feedback"), fontWeight = FontWeight.SemiBold) },
            text = {
                if (!submitted) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringRes("mine_feedback_submit_log_desc"),
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = feedbackText,
                            onValueChange = { feedbackText = it },
                            placeholder = { Text(stringRes("feedback_hint")) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { attachLogs = !attachLogs }
                        ) {
                            Checkbox(checked = attachLogs, onCheckedChange = { attachLogs = it })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringRes("mine_feedback_suggest_submit_log"), fontSize = 12.sp)
                        }
                    }
                } else {
                    Text(stringRes("feedback_success"))
                }
            },
            confirmButton = {
                if (!submitted) {
                    Button(
                        onClick = { submitted = true },
                        enabled = feedbackText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = XiaomiCyan)
                    ) {
                        Text(stringRes("mine_feedback_submit"))
                    }
                } else {
                    Button(onClick = { showFeedbackDialog = false }) {
                        Text(stringRes("close"))
                    }
                }
            },
            dismissButton = {
                if (!submitted) {
                    TextButton(onClick = { showFeedbackDialog = false }) {
                        Text(stringRes("cancel"))
                    }
                }
            }
        )
    }

    // 7. Version / Update Dialog
    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text(stringRes("mine_app_current_version"), fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Xiaomi Earbuds Desktop v1.37.1i", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${stringRes("device_settings_device_firmware_version")} 103701", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(stringRes("app_upgrade_no_newer_version"), fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showVersionDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = XiaomiCyan)) {
                    Text(stringRes("passport_i_know"))
                }
            }
        )
    }

    // 8. User Agreement Dialog
    if (showAgreementDialog) {
        AlertDialog(
            onDismissRequest = { showAgreementDialog = false },
            title = { Text(stringRes("mine_user_agreement"), fontWeight = FontWeight.SemiBold) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringRes("mine_user_agreement_content"),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAgreementDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = XiaomiCyan)) {
                    Text(stringRes("close"))
                }
            }
        )
    }

    // 9. Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(stringRes("mine_user_privacy_policy"), fontWeight = FontWeight.SemiBold) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringRes("mine_user_privacy_policy_content"),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = XiaomiCyan)) {
                    Text(stringRes("close"))
                }
            }
        )
    }

    // 10. Revoke Privacy Consent Dialog (1:1 with mine_revoke_privacy_granted_des2)
    if (showRevokeDialog) {
        AlertDialog(
            onDismissRequest = { showRevokeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource("drawable/mine_revoke_warn.webp"),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringRes("mine_revoke_privacy_granted"),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Text(
                    text = stringRes("mine_revoke_privacy_granted_des2"),
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        DevicePreferences.logout()
                        DevicePreferences.setUserExperienceAccepted(false)
                        isLoggedIn = false
                        userId = null
                        userName = null
                        userExperienceAccepted = false
                        showRevokeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = XiaomiRed)
                ) {
                    Text(stringRes("mine_verify_revoke"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeDialog = false }) {
                    Text(stringRes("cancel"))
                }
            }
        )
    }

    // 11. Experience Program Information Dialog
    if (showExperienceInfoDialog) {
        AlertDialog(
            onDismissRequest = { showExperienceInfoDialog = false },
            title = { Text(stringRes("mine_user_experience_improvement"), fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    text = stringRes("mine_user_experience_improvement_engage_detail"),
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(onClick = { showExperienceInfoDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = XiaomiCyan)) {
                    Text(stringRes("passport_i_know"))
                }
            }
        )
    }
}
