package com.alan.ximiearbuds.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alan.ximiearbuds.core.account.XiaomiAccountClient
import com.alan.ximiearbuds.ui.theme.LocalStrings
import com.alan.ximiearbuds.ui.theme.stringRes
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

/**
 * 1:1 Compose replica of Xiaomi Passport Login (AccountLoginActivity & passport_fragment_password_login.xml).
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                .width(420.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E1E),
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

                // Official Xiaomi squircle logo
                XiaomiLogoIcon(modifier = Modifier.size(56.dp))

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringRes("passport_mi_account_title"),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Text(
                    text = stringRes("passport_user_agreement_hint_first_login"),
                    fontSize = 12.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )

                // Error message banner
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF331515))
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

                // Status message banner (e.g. web login in progress)
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

                // Input 1: User ID / Email / Phone
                OutlinedTextField(
                    value = userIdInput,
                    onValueChange = {
                        userIdInput = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringRes("passport_input_phone_number_hint"), fontSize = 13.sp) },
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6700),
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedLabelColor = Color(0xFFFF6700),
                        unfocusedLabelColor = Color(0xFF888888),
                        focusedContainerColor = Color(0xFF141414),
                        unfocusedContainerColor = Color(0xFF141414)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Input 2: Password
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringRes("passport_input_password_hint"), fontSize = 13.sp) },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) stringRes("passport_password_not_show") else stringRes("passport_password_show"),
                                tint = Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6700),
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedLabelColor = Color(0xFFFF6700),
                        unfocusedLabelColor = Color(0xFF888888),
                        focusedContainerColor = Color(0xFF141414),
                        unfocusedContainerColor = Color(0xFF141414)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Agreement checkbox matching AgreementView
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { agreementAccepted = !agreementAccepted },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreementAccepted,
                        onCheckedChange = { agreementAccepted = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFFF6700),
                            uncheckedColor = Color(0xFF666666),
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringRes("passport_user_agreement_hint_default").replace(Regex("<[^>]*>"), "").replace("%1\$s", "").replace("%2\$s", "").trim(),
                        fontSize = 11.sp,
                        color = Color(0xFF9E9E9E),
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Button: Connexion
                Button(
                    onClick = {
                        if (!agreementAccepted) {
                            errorMessage = strings["passport_request_agree"]
                            return@Button
                        }
                        if (userIdInput.trim().isEmpty() || passwordInput.isEmpty()) {
                            errorMessage = strings["passport_input_phone_number_hint"]
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
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6700),
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
                        Text(stringRes("passport_dialog_doing_login"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text(stringRes("passport_password_login_btn"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Alternative: Web & QR Code Login
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFDDDDDD)
                    ),
                    enabled = !isLoading
                ) {
                    Text(
                        text = stringRes("passport_login_with_sns"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Secondary links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringRes("passport_register_by_local_phone_long_text"),
                        fontSize = 12.sp,
                        color = Color(0xFF888888),
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
                        color = Color(0xFF888888),
                        modifier = Modifier
                            .clickable {
                                openBrowser("https://account.xiaomi.com/pass/forgetPassword")
                            }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Crisp vector replica of Xiaomi orange squircle brand logo.
 */
@Composable
fun XiaomiLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val cornerRadiusPx = sizePx * 0.28f

        // Draw orange squircle background
        drawRoundRect(
            color = Color(0xFFFF6700),
            topLeft = Offset.Zero,
            size = Size(sizePx, sizePx),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )

        // Draw white "mi" glyph
        val strokeWidth = sizePx * 0.082f
        val halfStroke = strokeWidth / 2f
        val white = Color.White

        val startX = sizePx * 0.28f
        val midX1 = sizePx * 0.45f
        val midX2 = sizePx * 0.58f
        val endX = sizePx * 0.72f
        val topY = sizePx * 0.30f
        val bottomY = sizePx * 0.70f
        val archRadius = sizePx * 0.065f

        // Left 'm' stem
        drawLine(
            color = white,
            start = Offset(startX, topY),
            end = Offset(startX, bottomY),
            strokeWidth = strokeWidth
        )

        // Middle 'm' arch and center stem
        val path1 = Path().apply {
            moveTo(startX, topY + archRadius)
            quadraticTo(startX, topY, startX + archRadius, topY)
            lineTo(midX1 - archRadius, topY)
            quadraticTo(midX1, topY, midX1, topY + archRadius)
            lineTo(midX1, bottomY)
        }
        drawPath(path1, white, style = Stroke(width = strokeWidth))

        // Right 'm' inner arch and dot
        drawLine(
            color = white,
            start = Offset(midX2, topY),
            end = Offset(midX2, bottomY),
            strokeWidth = strokeWidth
        )

        // 'i' stem
        drawLine(
            color = white,
            start = Offset(endX, topY + strokeWidth * 1.6f),
            end = Offset(endX, bottomY),
            strokeWidth = strokeWidth
        )

        // 'i' dot
        drawCircle(
            color = white,
            radius = strokeWidth * 0.65f,
            center = Offset(endX, topY + strokeWidth * 0.35f)
        )
    }
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
