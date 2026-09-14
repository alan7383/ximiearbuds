package com.alan.ximiearbuds.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.Dialog
import com.alan.ximiearbuds.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class FitStatus {
    IDLE,
    TESTING,
    RESULT
}

/**
 * 1:1 Faithful Reproduction of official Fit Detection (device_settings_fragment_fit_detection.xml)
 */
@Composable
fun XiaomiFitDetectionDialog(
    onDismiss: () -> Unit
) {
    var fitStatus by remember { mutableStateOf(FitStatus.IDLE) }
    var leftFitGood by remember { mutableStateOf(true) }
    var rightFitGood by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = XiaomiPageBg,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringRes("device_settings_fit_detection"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = XiaomiTextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = XiaomiTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when (fitStatus) {
                        FitStatus.IDLE -> stringRes("device_settings_put_both_earbuds_into_ears_before_detecting")
                        FitStatus.TESTING -> stringRes("device_settings_keep_earbuds_fit_tight_to_ears")
                        FitStatus.RESULT -> stringRes("device_settings_fitness_detection_detail")
                    },
                    fontSize = 13.5.sp,
                    color = XiaomiTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Earbuds Image
                Box(
                    modifier = Modifier
                        .size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource("drawable/device_settings_ic_fit_detection_default.webp"),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Left & Right Detection Result Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Ear Indicator
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val leftIcon = when (fitStatus) {
                            FitStatus.IDLE, FitStatus.TESTING -> "drawable/device_settings_ic_left_fit_detection.webp"
                            FitStatus.RESULT -> if (leftFitGood) "drawable/device_settings_ic_left_fit_detection_well.webp" else "drawable/device_settings_ic_left_fit_detection_not_good.webp"
                        }
                        Image(
                            painter = painterResource(leftIcon),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringRes("device_settings_left"),
                            fontSize = 13.sp,
                            color = XiaomiTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        if (fitStatus == FitStatus.RESULT) {
                            Text(
                                text = if (leftFitGood) stringRes("device_settings_fitness_good") else stringRes("device_settings_fitness_not_good"),
                                fontSize = 11.5.sp,
                                color = if (leftFitGood) XiaomiGreen else Color(0xFFE53935)
                            )
                        }
                    }

                    // Right Ear Indicator
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val rightIcon = when (fitStatus) {
                            FitStatus.IDLE, FitStatus.TESTING -> "drawable/device_settings_ic_right_fit_detection.webp"
                            FitStatus.RESULT -> if (rightFitGood) "drawable/device_settings_ic_right_fit_detection_well.webp" else "drawable/device_settings_ic_right_fit_detection_not_good.webp"
                        }
                        Image(
                            painter = painterResource(rightIcon),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringRes("device_settings_right"),
                            fontSize = 13.sp,
                            color = XiaomiTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        if (fitStatus == FitStatus.RESULT) {
                            Text(
                                text = if (rightFitGood) stringRes("device_settings_fitness_good") else stringRes("device_settings_fitness_not_good"),
                                fontSize = 11.5.sp,
                                color = if (rightFitGood) XiaomiGreen else Color(0xFFE53935)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bottom Buttons
                if (fitStatus == FitStatus.TESTING) {
                    CircularProgressIndicator(
                        color = XiaomiOrange,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringRes("device_settings_detecting"),
                        fontSize = 14.sp,
                        color = XiaomiTextSecondary
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (fitStatus == FitStatus.RESULT) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringRes("device_settings_finish_fit_detection"),
                                    color = XiaomiTextPrimary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                fitStatus = FitStatus.TESTING
                                scope.launch {
                                    delay(2200)
                                    leftFitGood = true
                                    rightFitGood = true
                                    fitStatus = FitStatus.RESULT
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = XiaomiOrange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (fitStatus == FitStatus.IDLE) stringRes("device_settings_start_detecting") else stringRes("device_settings_detect_again"),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
