package com.alan.ximiearbuds.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.window.Dialog
import com.alan.ximiearbuds.core.account.XiaomiAccountClient
import com.alan.ximiearbuds.ui.theme.LocalStrings
import com.alan.ximiearbuds.ui.theme.XiaomiOrange
import com.alan.ximiearbuds.ui.theme.stringRes
import com.alan.ximiearbuds.ui.theme.parseHtmlLinks
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

/**
 * 1:1 Compose replica of Xiaomi Passport Login Dialog matching passport_fragment_password_login.xml.
 */
@Composable
fun MiuixLoginDialog(
    onDismissRequest: () -> Unit,
    onLoginSuccess: (userId: String, userName: String) -> Unit
) {
    val strings = LocalStrings.current
    var userIdInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var agreementAccepted by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = {
        if (!isLoading) {
            XiaomiAccountClient.stopWebLogin()
            onDismissRequest()
        }
    }) {
        Surface(
            modifier = Modifier
                .width(400.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E20),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            if (!isLoading) {
                                XiaomiAccountClient.stopWebLogin()
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringRes("close"),
                            tint = Color(0xFF888888),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Official Xiaomi squircle logo (passport_auth_logo: 48dp)
                Image(
                    painter = painterResource("drawable/passport_auth_logo.png"),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Title: "Se connecter avec Compte Xiaomi" (NO subtitle)
                Text(
                    text = stringRes("passport_mi_account_title"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error message banner
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF3B1A1A))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = msg,
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Status message banner
                AnimatedVisibility(visible = statusMessage != null) {
                    statusMessage?.let { status ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1B2B3A))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
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
                                    color = Color(0xFF74C0FC),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Input 1: User ID / Email / Phone (passport_layout_edit_text_group_view.xml)
                DialogInputField(
                    value = userIdInput,
                    onValueChange = {
                        userIdInput = it
                        errorMessage = null
                    },
                    hintText = stringRes("passport_user_id_hint"),
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Email
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Input 2: Password (passport_layout_edit_text_group_view.xml)
                DialogInputField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        errorMessage = null
                    },
                    hintText = stringRes("passport_user_password_hint"),
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Password
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Agreement checkbox matching AgreementView
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

                    Spacer(modifier = Modifier.width(8.dp))

                    val agreementTemplate = remember(strings) {
                        strings["passport_user_agreement_hint_default", "https://privacy.mi.com/all/fr_FR/", "https://account.xiaomi.com/about/protocol/privacy"]
                    }
                    val agreementAnnotated = remember(agreementTemplate) {
                        parseHtmlLinks(agreementTemplate)
                    }

                    Text(
                        text = agreementAnnotated,
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E93),
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Button: "Se connecter" (pill shape, orange)
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
                        .height(46.dp),
                    shape = RoundedCornerShape(23.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6900),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF4A3020),
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
                        Text(stringRes("passport_dialog_doing_login"), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(stringRes("passport_password_login_btn"), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Secondary Links: Créer un compte | Mot de passe oublié ? | SMS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringRes("passport_register_by_local_phone_short_text"),
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier
                            .clickable {
                                openBrowser("https://account.xiaomi.com/pass/register")
                            }
                            .padding(4.dp)
                    )

                    Text(
                        text = " | ",
                        fontSize = 12.sp,
                        color = Color(0xFF444444)
                    )

                    Text(
                        text = stringRes("passport_to_forget_password"),
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier
                            .clickable {
                                openBrowser("https://account.xiaomi.com/pass/forgetPassword")
                            }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Footer Divider: Autres méthodes pour se connecter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Color(0xFF333336))
                    )

                    Text(
                        text = stringRes("passport_login_with_sns"),
                        fontSize = 11.sp,
                        color = Color(0xFF777777),
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Color(0xFF333336))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Web / QR Code Login Button
                OutlinedButton(
                    onClick = {
                        if (!agreementAccepted) {
                            errorMessage = strings["passport_request_agree"]
                            return@OutlinedButton
                        }
                        isLoading = true
                        errorMessage = null

                        scope.launch {
                            XiaomiAccountClient.startWebLogin(
                                onStatusChange = { status ->
                                    statusMessage = status
                                },
                                onComplete = { result ->
                                    isLoading = false
                                    statusMessage = null
                                    when (result) {
                                        is XiaomiAccountClient.AuthResult.Success -> {
                                            onLoginSuccess(result.userId, result.userName)
                                        }
                                        is XiaomiAccountClient.AuthResult.Error -> {
                                            errorMessage = result.message
                                        }
                                        else -> {}
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A3C)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFDDDDDD)
                    ),
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Connexion via Navigateur Web / Code QR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogInputField(
    value: String,
    onValueChange: (String) -> Unit,
    hintText: String,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: () -> Unit = {},
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF28282B))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = hintText,
                    color = Color(0xFF8E8E93),
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(Color(0xFFFF6900)),
                visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (value.isNotEmpty() && enabled) {
            Image(
                painter = painterResource("drawable/passport_edit_text_clear_all.png"),
                contentDescription = stringRes("passport_clear_input"),
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") }
            )
            Spacer(modifier = Modifier.width(6.dp))
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
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { onTogglePasswordVisibility() }
            )
        }
    }
}

/**
 * Crisp vector replica of Xiaomi orange squircle brand logo.
 */
@Composable
fun XiaomiLogoIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource("drawable/passport_auth_logo.png"),
        contentDescription = null,
        modifier = modifier
    )
}

private fun openBrowser(url: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            Runtime.getRuntime().exec(arrayOf("xdg-open", url))
        }
    } catch (e: Exception) {
        println("MiuixLoginDialog: openBrowser failed: ${e.message}")
    }
}
