package com.alan.ximiearbuds.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.account.XiaomiAccountClient
import com.alan.ximiearbuds.ui.theme.*
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

/**
 * 1:1 authentic reproduction of Xiaomi Passport AccountLoginActivity:
 * - Layout wrapper: passport_activity_layout_wrapper.xml
 * - Header: passport_layout_page_header.xml (Back, Title, Help)
 * - Main: passport_fragment_password_login.xml (Logo, Title, EditTextGroupView, AgreementView, HighlightRoundButton, Links Flow)
 * - Footer: passport_fragment_sns_login.xml (Divider, Other login methods / Web OAuth)
 */
@Composable
fun MiuixLoginScreen(
    onBackClick: () -> Unit,
    onLoginSuccess: (userId: String, userName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    var userIdInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var agreementAccepted by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val bgColor = if (isDark) XiaomiPageBg else XiaomiLightPageBg
    val textPrimary = if (isDark) XiaomiTextPrimary else XiaomiLightTextPrimary
    val textSecondary = if (isDark) XiaomiTextSecondary else XiaomiLightTextSecondary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // =========================================================================
            // 1. Page Header (passport_layout_page_header.xml)
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button (ic_base_back.webp, with ColorFilter tint for dark/light mode)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (!isLoading) {
                                XiaomiAccountClient.stopWebLogin()
                                onBackClick()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource("drawable/ic_base_back.webp"),
                        contentDescription = stringRes("passport_back"),
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(textPrimary)
                    )
                }

                // Help button (passport_help: "Aide")
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            showHelpDialog = true
                            openBrowser("https://account.xiaomi.com/helpcenter")
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringRes("passport_help"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }

            // =========================================================================
            // 2. Scrollable Body & Footer (FlexVerticalLinearLayout)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                // Official Squircle Mi Logo (passport_auth_logo: 48dp)
                Image(
                    painter = painterResource("drawable/passport_auth_logo.png"),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Title: "Se connecter avec Compte Xiaomi" (18sp bold, NO subtitle!)
                Text(
                    text = stringRes("passport_mi_account_title"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Error message banner
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF3B1A1A) else Color(0xFFFFECEC))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = msg,
                                color = Color(0xFFFF5252),
                                fontSize = 13.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Status message banner (e.g. web login in progress)
                AnimatedVisibility(visible = statusMessage != null) {
                    statusMessage?.let { status ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF1B2B3A) else Color(0xFFE7F3FF))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF339AF0)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = status,
                                    color = if (isDark) Color(0xFF74C0FC) else Color(0xFF1971C2),
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                // Input 1: User ID / Email / Phone (passport_layout_edit_text_group_view.xml)
                XiaomiPassportInputField(
                    value = userIdInput,
                    onValueChange = {
                        userIdInput = it
                        errorMessage = null
                    },
                    hintText = stringRes("passport_user_id_hint"),
                    isDark = isDark,
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Email
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Input 2: Password (passport_layout_edit_text_group_view.xml)
                XiaomiPassportInputField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        errorMessage = null
                    },
                    hintText = stringRes("passport_user_password_hint"),
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                    isDark = isDark,
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Password,
                    onDone = {
                        // Trigger login if inputs ready
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Agreement Checkbox & Text (passport_layout_agreement_view.xml)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { agreementAccepted = !agreementAccepted }
                        ),
                    verticalAlignment = Alignment.Top
                ) {
                    Image(
                        painter = painterResource(
                            if (agreementAccepted) "drawable/passport_ic_checkbox_checked.png"
                            else "drawable/passport_ic_checkbox_unchecked.png"
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    val agreementAnnotated = remember(strings) {
                        buildAnnotatedString {
                            append("J'ai lu et accepté l'")
                            pushStringAnnotation(tag = "AGREEMENT", annotation = "https://privacy.mi.com/all/fr_FR/")
                            withStyle(style = SpanStyle(color = Color(0xFF1F93FF), fontWeight = FontWeight.Medium)) {
                                append("Accord Utilisateur")
                            }
                            pop()
                            append(" et ")
                            pushStringAnnotation(tag = "PRIVACY", annotation = "https://account.xiaomi.com/about/protocol/privacy")
                            withStyle(style = SpanStyle(color = Color(0xFF1F93FF), fontWeight = FontWeight.Medium)) {
                                append("Politique de Confidentialité")
                            }
                            pop()
                            append(" du Compte Xiaomi.")
                        }
                    }

                    Text(
                        text = agreementAnnotated,
                        fontSize = 12.sp,
                        color = textSecondary,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Button: "Se connecter" (style=@style/PassportWidget.HighlightRoundButton)
                Button(
                    onClick = {
                        if (!agreementAccepted) {
                            errorMessage = strings["passport_request_agree"]
                            return@Button
                        }
                        if (userIdInput.trim().isEmpty() || passwordInput.isEmpty()) {
                            errorMessage = strings["passport_error_empty_user_id"]
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null
                        statusMessage = strings["passport_checking_account"]

                        scope.launch {
                            val result = XiaomiAccountClient.loginWithPassword(userIdInput, passwordInput)
                            isLoading = false
                            statusMessage = null

                            when (result) {
                                is XiaomiAccountClient.AuthResult.Success -> {
                                    onLoginSuccess(result.userId, result.userName)
                                }
                                is XiaomiAccountClient.AuthResult.NeedVerification -> {
                                    errorMessage = result.message
                                }
                                is XiaomiAccountClient.AuthResult.NeedCaptcha -> {
                                    errorMessage = result.message
                                }
                                is XiaomiAccountClient.AuthResult.Error -> {
                                    errorMessage = result.message
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6900),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF5A3010),
                        disabledContentColor = Color(0xFF888888)
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading && statusMessage != null && !statusMessage!!.contains("navigateur")) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringRes("passport_dialog_doing_login"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            stringRes("passport_password_login_btn"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Flow Links: goto_h5_register | find_password | verify_code_login
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringRes("passport_register_by_local_phone_short_text"),
                        fontSize = 13.sp,
                        color = textSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                openBrowser("https://account.xiaomi.com/pass/register")
                            }
                            .padding(4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .width(1.dp)
                            .height(12.dp)
                            .background(textSecondary.copy(alpha = 0.4f))
                    )

                    Text(
                        text = stringRes("passport_to_forget_password"),
                        fontSize = 13.sp,
                        color = textSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                openBrowser("https://account.xiaomi.com/pass/forgetPassword")
                            }
                            .padding(4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .width(1.dp)
                            .height(12.dp)
                            .background(textSecondary.copy(alpha = 0.4f))
                    )

                    Text(
                        text = stringRes("passport_to_verify_code_login"),
                        fontSize = 13.sp,
                        color = textSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                // Launch web login flow
                                startWebLoginFlow(scope, { isLoading = it }, { statusMessage = it }, { errorMessage = it }, onLoginSuccess)
                            }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // =========================================================================
                // 3. Footer: Autres méthodes pour se connecter (passport_fragment_sns_login.xml)
                // =========================================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(if (isDark) Color(0xFF333336) else Color(0xFFE0E0E0))
                    )

                    Text(
                        text = stringRes("passport_login_with_sns"),
                        fontSize = 12.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(if (isDark) Color(0xFF333336) else Color(0xFFE0E0E0))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Clean pill action button for Web & QR Code authentication
                OutlinedButton(
                    onClick = {
                        if (!agreementAccepted) {
                            errorMessage = strings["passport_request_agree"]
                            return@OutlinedButton
                        }
                        startWebLoginFlow(scope, { isLoading = it }, { statusMessage = it }, { errorMessage = it }, onLoginSuccess)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textPrimary
                    ),
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Connexion via Navigateur Web / Code QR",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        // =========================================================================
        // 4. Xiaomi Account Help Dialog
        // =========================================================================
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = {
                    Text(
                        text = "Aide — Compte Xiaomi",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = textPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Assistance et dépannage de connexion :",
                            fontSize = 14.sp,
                            color = textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "• Mot de passe oublié : Vous pouvez réinitialiser votre mot de passe instantanément sur le portail officiel Xiaomi.",
                            fontSize = 13.sp,
                            color = textSecondary,
                            lineHeight = 17.sp
                        )
                        Text(
                            text = "• Codes ou SMS non reçus : Utilisez la méthode 'Connexion via Navigateur Web / Code QR' en bas de page pour vous connecter directement avec l'application Xiaomi ou Google.",
                            fontSize = 13.sp,
                            color = textSecondary,
                            lineHeight = 17.sp
                        )
                        Text(
                            text = "• Identifiant : Utilisez indifféremment votre adresse e-mail, numéro de téléphone international (+33...) ou votre ID Xiaomi.",
                            fontSize = 13.sp,
                            color = textSecondary,
                            lineHeight = 17.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            openBrowser("https://account.xiaomi.com/helpcenter")
                            showHelpDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XiaomiOrange)
                    ) {
                        Text("Consulter l'aide en ligne")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showHelpDialog = false }) {
                        Text(stringRes("close"))
                    }
                }
            )
        }
    }
}

/**
 * 1:1 Compose replica of Xiaomi Passport EditTextGroupView (passport_layout_edit_text_group_view.xml).
 * High fidelity solid rounded rectangle with clear button and password visibility toggle.
 */
@Composable
private fun XiaomiPassportInputField(
    value: String,
    onValueChange: (String) -> Unit,
    hintText: String,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: () -> Unit = {},
    isDark: Boolean = true,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onDone: () -> Unit = {}
) {
    val fieldBg = if (isDark) Color(0xFF222224) else Color(0xFFF2F2F7)
    val textColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val hintColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fieldBg)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = hintText,
                    color = hintColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(Color(0xFFFF6900)),
                visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Trailing actions: Clear text icon & Password toggle icon
        if (value.isNotEmpty() && enabled) {
            Image(
                painter = painterResource("drawable/passport_edit_text_clear_all.png"),
                contentDescription = stringRes("passport_clear_input"),
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (isPassword) {
            Image(
                painter = painterResource(
                    if (passwordVisible) "drawable/passport_password_show.png"
                    else "drawable/passport_password_not_show.png"
                ),
                contentDescription = stringRes(
                    if (passwordVisible) "passport_password_show" else "passport_password_not_show"
                ),
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { onTogglePasswordVisibility() }
            )
        }
    }
}

private fun startWebLoginFlow(
    scope: kotlinx.coroutines.CoroutineScope,
    setLoading: (Boolean) -> Unit,
    setStatus: (String?) -> Unit,
    setError: (String?) -> Unit,
    onSuccess: (String, String) -> Unit
) {
    setLoading(true)
    setError(null)
    scope.launch {
        XiaomiAccountClient.startWebLogin(
            onStatusChange = { status ->
                setStatus(status)
            },
            onComplete = { result ->
                setLoading(false)
                setStatus(null)
                when (result) {
                    is XiaomiAccountClient.AuthResult.Success -> {
                        onSuccess(result.userId, result.userName)
                    }
                    is XiaomiAccountClient.AuthResult.Error -> {
                        setError(result.message)
                    }
                    else -> {}
                }
            }
        )
    }
}

private fun openBrowser(url: String) {
    try {
        val os = System.getProperty("os.name", "").lowercase()
        when {
            os.contains("win") -> {
                ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start()
            }
            os.contains("mac") -> {
                ProcessBuilder("open", url).start()
            }
            else -> {
                // Linux (KDE Wayland, GNOME, etc.)
                var started = false
                val commands = listOf(
                    listOf("xdg-open", url),
                    listOf("kde-open5", url),
                    listOf("kde-open", url),
                    listOf("gio", "open", url)
                )
                for (cmd in commands) {
                    try {
                        val pb = ProcessBuilder(cmd)
                        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        pb.redirectError(ProcessBuilder.Redirect.DISCARD)
                        pb.start()
                        started = true
                        break
                    } catch (ignored: Exception) {}
                }
                if (!started && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI(url))
                }
            }
        }
    } catch (e: Exception) {
        println("openBrowser failed: ${e.message}")
    }
}
