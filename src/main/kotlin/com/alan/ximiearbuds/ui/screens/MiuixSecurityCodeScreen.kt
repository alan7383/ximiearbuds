package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 Faithful Reproduction of Xiaomi Earbuds `mine_fragment_security_code.xml` & `SecurityCodeFragment.java`.
 * Allows users to verify official genuine Xiaomi Earbuds via 20-digit security code.
 */
import com.alan.ximiearbuds.core.account.XiaomiAccountClient
import kotlinx.coroutines.launch

@Composable
fun MiuixSecurityCodeScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var rawInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var verificationResult by remember { mutableStateOf<Int?>(null) } // null = input phase, Int = query count
    var isChecking by remember { mutableStateOf(false) }
    var showFailDialog by remember { mutableStateOf(false) }

    val securityCodeErrorText = stringRes("mine_security_code_error")
    val verifyTimesFormat = stringRes("mine_security_verify_times")

    val performCheck = {
        val clean = rawInput.filter { !it.isWhitespace() }
        if (clean.length != 20) {
            errorMessage = securityCodeErrorText
        } else {
            isChecking = true
            errorMessage = null
            scope.launch {
                val res = XiaomiAccountClient.verifySecurityCode(clean)
                isChecking = false
                if (res.isGenuine) {
                    verificationResult = res.queryCount
                } else {
                    showFailDialog = true
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        MiuixTopAppBar(
            title = stringRes("mine_security_title"),
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (verificationResult == null) {
                // Input Phase (security_layout in mine_fragment_security_code.xml)
                Text(
                    text = stringRes("mine_security_tip_text"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Input Field (security_code_edit)
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { input ->
                        // Filter to digits and spaces only
                        rawInput = input.filter { it.isDigit() || it.isWhitespace() }
                        errorMessage = null
                    },
                    placeholder = {
                        Text(
                            text = stringRes("mine_security_notify_text"),
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { performCheck() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XiaomiCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                        cursorColor = XiaomiCyan,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = XiaomiRed,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))
                Spacer(modifier = Modifier.height(48.dp))

                // Action Button (immediately_btn: BaseButton.Positive)
                Button(
                    onClick = { performCheck() },
                    enabled = rawInput.filter { !it.isWhitespace() }.isNotEmpty() && !isChecking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = XiaomiCyan,
                        contentColor = Color.White,
                        disabledContainerColor = XiaomiCyan.copy(alpha = 0.3f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = stringRes("mine_security_title_immediately"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Verification Result Phase (security_result_layout in mine_fragment_security_code.xml)
                Spacer(modifier = Modifier.height(32.dp))

                Image(
                    painter = painterResource("drawable/mine_security_success.png"),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringRes("mine_security_verify_success_text"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                val timesText = if (verifyTimesFormat.contains("%1\$d") || verifyTimesFormat.contains("%d")) {
                    verifyTimesFormat.replace("%1\$d", "${verificationResult ?: 1}").replace("%d", "${verificationResult ?: 1}")
                } else {
                    "$verifyTimesFormat : ${verificationResult ?: 1}"
                }

                Text(
                    text = timesText,
                    fontSize = 14.sp,
                    color = XiaomiCyan,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringRes("mine_security_notify_query_much_text"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringRes("mine_security_notifyto_verify"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(48.dp))

                OutlinedButton(
                    onClick = {
                        verificationResult = null
                        rawInput = ""
                        errorMessage = null
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = XiaomiCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = stringRes("device_manager_connect_again"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showFailDialog) {
        AlertDialog(
            onDismissRequest = { showFailDialog = false },
            title = {
                Text(
                    text = stringRes("mine_security_code_not_exit"),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = stringRes("mine_security_code_error"),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showFailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = XiaomiCyan)
                ) {
                    Text(stringRes("mine_login_limit_confirm"))
                }
            }
        )
    }
}
