package com.alan.ximiearbuds.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.core.protocol.FitDetectionState
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.theme.XiaomiCyan
import com.alan.ximiearbuds.ui.theme.stringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage

private val XiaomiGreen = Color(0xFF20D571)
private val XiaomiOrange = Color(0xFFFF9500)
private val NeutralBtnBg = Color(0xFF242426)

/**
 * 1:1 authentic replica of Xiaomi Earbuds `device_settings_fragment_fit_detection.xml`,
 * `FitDetectionViewModel.java`, and `FitDetectionModel.java`.
 *
 * Implements:
 * - Dynamic official model-specific graphic (e.g. Redmi Buds 6 Pro `devices/fit/2717_509e_c*.png`)
 * - Official earbud status badges (`device_settings_ic_left/right_fit_detection[_well|_not_good].webp`)
 * - Dynamic descriptions & verdict text
 * - Official 4-dot pulsating loading animation (`loading.json`)
 * - Official bottom button dual-layout ("Terminé" / "Redémarrer")
 * - Official error & warning modal dialogs (ear out, calling, timeout)
 */
@Composable
fun MiuixFitDetectionScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fitState by controller.fitDetection.collectAsState()
    val activeModel by controller.activeModel.collectAsState()
    val deviceInfo by controller.deviceInfo.collectAsState()
    val scrollState = rememberScrollState()

    val titleText = stringRes("device_settings_fit_detection")

    // Dynamic description matching FitDetectionModel.java:360 getDetectionResultText()
    val descriptionText = when (fitState.status) {
        FitDetectionState.STATUS_TIP -> stringRes("device_settings_fit_detection_des")
        FitDetectionState.STATUS_NOT_START -> stringRes("device_settings_start_fit_detection_tips")
        FitDetectionState.STATUS_DETECTING -> stringRes("device_settings_fit_detecting_des")
        FitDetectionState.STATUS_FINISH -> {
            val leftWell = fitState.leftResult == FitDetectionState.FIT_WELL
            val rightWell = fitState.rightResult == FitDetectionState.FIT_WELL
            when {
                leftWell && rightWell -> stringRes("device_settings_fit_detection_well_des")
                !leftWell && rightWell -> stringRes("device_settings_fit_detection_left_not_good_des")
                leftWell && !rightWell -> stringRes("device_settings_fit_detection_right_not_good_des")
                else -> stringRes("device_settings_fit_detection_not_good_des")
            }
        }
        else -> stringRes("device_settings_fit_detection_des")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Navigation Bar
            MiuixTopAppBar(
                title = titleText,
                onBackClick = {
                    if (fitState.isRunning) {
                        controller.stopFitDetection()
                    }
                    onBackClick()
                }
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // Title: device_settings_fit_detection (22sp Bold)
                Text(
                    text = titleText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle / Dynamic instruction description (13.5sp)
                Text(
                    text = descriptionText,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Center Graphic: Official pair of earbuds for this exact model (Redmi Buds 6 Pro, etc.)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    FitDetectionDeviceImage(
                        model = activeModel,
                        colorType = deviceInfo.colorType,
                        modifier = Modifier.size(width = 240.dp, height = 114.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Left & Right Earbud Status Indicators aligned precisely underneath each earbud stem/tip
                Box(
                    modifier = Modifier.width(240.dp)
                ) {
                    // Left earbud indicator (centered at 27.0%)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (240.dp * 0.270f) - 48.dp)
                            .width(96.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        EarbudFitIndicator(
                            isLeft = true,
                            result = fitState.leftResult,
                            isFinished = fitState.isFinished
                        )
                    }

                    // Right earbud indicator (centered at 73.4%)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (240.dp * 0.734f) - 48.dp)
                            .width(96.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        EarbudFitIndicator(
                            isLeft = false,
                            result = fitState.rightResult,
                            isFinished = fitState.isFinished
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Fixed Bottom Footer (pinned to bottom of screen, exactly like Xiaomi ConstraintLayout)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pulsating 4-dot wave loading indicator (height 26dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (fitState.isRunning) {
                        XiaomiPulsatingLoadingWave()
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                if (fitState.isFinished) {
                    // Two buttons side by side: [Terminé] [Redémarrer]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Finish button (Secondary / Grey)
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeutralBtnBg,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = stringRes("device_settings_finish_fit_detection"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Restart button (Primary / Cyan)
                        Button(
                            onClick = { controller.startFitDetection() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = XiaomiCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = stringRes("device_settings_fit_redetection"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Single full-width primary button
                    val actionBtnText = when (fitState.status) {
                        FitDetectionState.STATUS_TIP -> stringRes("device_settings_continue")
                        FitDetectionState.STATUS_NOT_START -> stringRes("device_settings_start_fit_detection")
                        FitDetectionState.STATUS_DETECTING -> stringRes("device_settings_fit_detecting")
                        else -> stringRes("device_settings_start_fit_detection")
                    }

                    Button(
                        onClick = {
                            when (fitState.status) {
                                FitDetectionState.STATUS_TIP -> {
                                    controller.setFitDetectionStatus(FitDetectionState.STATUS_NOT_START)
                                }
                                FitDetectionState.STATUS_NOT_START -> {
                                    controller.startFitDetection()
                                }
                                FitDetectionState.STATUS_DETECTING -> {
                                    controller.stopFitDetection()
                                }
                                else -> {
                                    controller.startFitDetection()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = XiaomiCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = actionBtnText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Official Warning / Error Tips Dialog
        if (fitState.errorCode != FitDetectionState.CODE_NONE) {
            val errorMsg = when (fitState.errorCode) {
                FitDetectionState.CODE_EAR_OUT -> stringRes("device_settings_fit_detection_ear_out_tips")
                FitDetectionState.CODE_CALLING -> stringRes("device_settings_fit_detection_calling_tips")
                FitDetectionState.CODE_DISCONNECT -> stringRes("device_settings_device_not_connected")
                FitDetectionState.CODE_TIMEOUT -> stringRes("device_settings_fit_detection_ear_out_tips")
                else -> stringRes("device_settings_fit_detection_ear_out_tips")
            }

            XiaomiFitTipsDialog(
                title = stringRes("device_settings_fit_detection_dialog_title"),
                message = errorMsg,
                confirmText = stringRes("device_settings_fit_detection_dialog_confirm"),
                onDismiss = { controller.dismissFitDetectionError() }
            )
        }
    }
}

/**
 * Model-specific fit detection image loader.
 * Directly renders the official Xiaomi fit image for the connected device (e.g. Redmi Buds 6 Pro).
 */
@Composable
private fun FitDetectionDeviceImage(
    model: EarbudsModel?,
    colorType: Int,
    modifier: Modifier = Modifier
) {
    val effectiveColor = if (colorType > 0) colorType else (model?.defaultColor ?: 2)
    val modelKey = model?.iconFile?.substringBeforeLast(".")?.ifBlank { "2717_509e" } ?: "2717_509e"

    val candidatePaths = remember(modelKey, effectiveColor) {
        listOf(
            "devices/fit/${modelKey}_c$effectiveColor.png",
            "devices/fit/${modelKey}_c${model?.defaultColor ?: 2}.png",
            "devices/fit/${modelKey}_c5.png",
            "devices/fit/${modelKey}_c2.png",
            "devices/fit/${modelKey}_c1.png",
            "devices/fit/2717_509e_c$effectiveColor.png",
            "devices/fit/2717_509e_c5.png",
            "devices/fit/2717_509e_c2.png",
            "devices/fit/2717_509e_c1.png"
        )
    }

    var imageBitmap by remember(modelKey, effectiveColor) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(candidatePaths) {
        withContext(Dispatchers.IO) {
            val cl = Thread.currentThread().contextClassLoader
            for (path in candidatePaths) {
                try {
                    val stream = cl.getResourceAsStream(path)
                    if (stream != null) {
                        val bytes = stream.use { it.readAllBytes() }
                        val bm = SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                        imageBitmap = bm
                        break
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val bm = imageBitmap
    if (bm != null) {
        Image(
            bitmap = bm,
            contentDescription = model?.commercialName ?: "Redmi Buds 6 Pro",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        // Fallback to Redmi Buds 6 Pro official fit image
        Image(
            painter = painterResource("devices/fit/2717_509e_c1.png"),
            contentDescription = model?.commercialName ?: "Redmi Buds 6 Pro",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Individual earbud status badge and label (left or right).
 */
@Composable
private fun EarbudFitIndicator(
    isLeft: Boolean,
    result: Int,
    isFinished: Boolean
) {
    val iconPath = when {
        !isFinished -> if (isLeft) "drawable/device_settings_ic_left_fit_detection.webp" else "drawable/device_settings_ic_right_fit_detection.webp"
        result == FitDetectionState.FIT_WELL -> if (isLeft) "drawable/device_settings_ic_left_fit_detection_well.webp" else "drawable/device_settings_ic_right_fit_detection_well.webp"
        else -> if (isLeft) "drawable/device_settings_ic_left_fit_detection_not_good.webp" else "drawable/device_settings_ic_right_fit_detection_not_good.webp"
    }

    val statusText = when {
        !isFinished -> ""
        result == FitDetectionState.FIT_WELL -> stringRes("device_settings_fit_detection_well")
        isLeft -> stringRes("device_settings_adjust_left_earbud_position")
        else -> stringRes("device_settings_adjust_right_earbud_position")
    }

    val textColor = when {
        result == FitDetectionState.FIT_WELL -> XiaomiGreen
        else -> XiaomiOrange
    }

    Column(
        modifier = Modifier.width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(iconPath),
            contentDescription = if (isLeft) "Left Earbud" else "Right Earbud",
            modifier = Modifier.size(28.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (statusText.isNotBlank()) {
            Text(
                text = statusText,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        } else {
            // Keep height uniform during idle/detecting phases
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 1:1 replica of Xiaomi Lottie pulsating loading dots (`lottie/loading.json`).
 * Renders 4 cyan pulsating dots with smooth staggered scale & alpha waves.
 */
@Composable
private fun XiaomiPulsatingLoadingWave() {
    val infiniteTransition = rememberInfiniteTransition()

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 4) {
            val animProgress by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(offsetMillis = i * 140)
                )
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(animProgress)
                    .clip(CircleShape)
                    .background(XiaomiCyan.copy(alpha = 0.35f + animProgress * 0.65f))
            )
        }
    }
}

/**
 * Official Xiaomi styled Attention / Tips Dialog.
 */
@Composable
private fun XiaomiFitTipsDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E20),
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(min = 280.dp, max = 340.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    fontSize = 13.5.sp,
                    color = Color(0xFFC0C0C0),
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = XiaomiCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Text(
                        text = confirmText.trim(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
