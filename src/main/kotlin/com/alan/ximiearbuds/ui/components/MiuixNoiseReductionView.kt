package com.alan.ximiearbuds.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.alan.ximiearbuds.core.device.AncCapabilities
import com.alan.ximiearbuds.core.protocol.AncLevel
import com.alan.ximiearbuds.core.protocol.NoiseControlState
import com.alan.ximiearbuds.ui.theme.stringRes
import com.alan.ximiearbuds.core.protocol.NoiseMode
import com.alan.ximiearbuds.core.protocol.TransparencyLevel

/**
 * Authentic 1:1 replica of Xiaomi Earbuds `device_settings_layout_noise_redution.xml`.
 * 
 * Order matching official XML hierarchy:
 * 1. RadioGroup: Transparency, ANC, Off (device_settings_selector_trans, noise, noise_close)
 * 2. Divider
 * 3. switch_btn_smart_denoise: "Annulation du bruit intelligente" (Config 102, Function 1008)
 * 4. levelContainer:
 *    - auto_button: "Annulation adaptative du bruit" (Config 37, Function 1003 auto_fit)
 *    - levelDesView: Centered level description (if gears <= 9)
 *    - seekA + seekbar + levelView + seekS (23dp capsule track #2F2E32/#E2E2E6, thumb device_settings_ic_noise_reduction_btn.webp)
 *    - mild_tv & deep_tv: "Légère" & "Profonde" (if gears > 9)
 * 5. personalized_noise_reduction_switch: "Réduction personnalisée du bruit" (Config 59, Function 3012, marginTop 20dp)
 */
@Composable
fun MiuixNoiseReductionView(
    noiseState: NoiseControlState,
    capabilities: AncCapabilities = AncCapabilities(),
    onModeChange: (NoiseMode) -> Unit,
    onAncLevelChange: (Int) -> Unit,
    onTransparencyLevelChange: (Int) -> Unit,
    onAdaptiveAncChange: (Boolean) -> Unit,
    onSmartDenoiseChange: (Boolean) -> Unit,
    onPersonalizedAncChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val cardBg = if (isDark) Color(0xFF1E1E22) else Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .padding(vertical = 20.dp, horizontal = 16.dp)
    ) {
        // 1. Row of Radio Buttons: Transparency (if supported), ANC, Off
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            if (!capabilities.isSingleToggleOnly) {
                MiuixNoiseRadioItem(
                    label = stringRes("device_settings_noise_reduction_transparent"),
                    isSelected = noiseState.mode == NoiseMode.TRANSPARENCY,
                    normalDrawable = "drawable/device_settings_trans.webp",
                    checkedDrawable = "drawable/device_settings_trans_checked.webp",
                    onClick = { onModeChange(NoiseMode.TRANSPARENCY) },
                    modifier = Modifier.weight(1f)
                )
            }

            MiuixNoiseRadioItem(
                label = stringRes("device_settings_noise_reduction_open"),
                isSelected = noiseState.mode == NoiseMode.ANC,
                normalDrawable = "drawable/device_settings_noise.webp",
                checkedDrawable = "drawable/device_settings_noise_checked.webp",
                onClick = { onModeChange(NoiseMode.ANC) },
                modifier = Modifier.weight(1f)
            )

            MiuixNoiseRadioItem(
                label = stringRes("device_settings_noise_reduction_close"),
                isSelected = noiseState.mode == NoiseMode.OFF,
                normalDrawable = "drawable/device_settings_noise_close.webp",
                checkedDrawable = "drawable/device_settings_noise_close_checked.webp",
                onClick = { onModeChange(NoiseMode.OFF) },
                modifier = Modifier.weight(1f)
            )
        }

        // 2. Sub-controls for Active Noise Cancellation
        AnimatedVisibility(
            visible = noiseState.mode == NoiseMode.ANC,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                // Divider (device_settings_layout_noise_redution.xml line 48)
                HorizontalDivider(
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                // 2a. switch_btn_smart_denoise (Config 102, Function 1008)
                if (capabilities.hasSmartDenoise) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSmartDenoiseChange(!noiseState.isSmartDenoise) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringRes("device_settings_smart_denoise"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Switch(
                            checked = noiseState.isSmartDenoise,
                            onCheckedChange = onSmartDenoiseChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF007AFF)
                            )
                        )
                    }
                }

                // 2b. levelContainer (device_settings_layout_noise_redution.xml line 63)
                val ancGears = capabilities.ancLevels.ifEmpty { listOf(1, 0, 2) }
                if (ancGears.size > 1 || capabilities.hasAdaptiveAnc) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // auto_button: "Annulation adaptative du bruit" (Config 37, line 69)
                        if (capabilities.hasAdaptiveAnc) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAdaptiveAncChange(!noiseState.isAutoNoise) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringRes("device_settings_noise_reduction_adaptive"),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                Switch(
                                    checked = noiseState.isAutoNoise,
                                    onCheckedChange = onAdaptiveAncChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF007AFF)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // levelDesView & Slider
                        val depthDescription = when (noiseState.ancLevel) {
                            AncLevel.LIGHT -> stringRes("device_settings_mild")
                            AncLevel.BALANCED -> stringRes("device_settings_balanced")
                            AncLevel.DEEP, AncLevel.DEEP_PLUS -> stringRes("device_settings_deep")
                            AncLevel.ADAPTIVE -> stringRes("device_settings_noise_reduction_adaptive")
                            AncLevel.ANTIWIND -> stringRes("device_settings_wind_resistance")
                        }

                        if (ancGears.size > 9) {
                            OfficialNoiseSlider(
                                gears = ancGears,
                                currentGear = noiseState.ancLevelIndex,
                                showDots = false,
                                showLevelDes = false,
                                levelDesText = depthDescription,
                                lowIconRes = "drawable/device_settings_reduct_low.webp",
                                highIconRes = "drawable/device_settings_reduct_high.webp",
                                enabled = !noiseState.isAutoNoise,
                                onLevelSelected = onAncLevelChange
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // mild_tv & deep_tv (line 128 & 139)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringRes("device_settings_mild"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                                    color = Color(0xFF8C8C8C)
                                )
                                Text(
                                    text = stringRes("device_settings_deep"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                                    color = Color(0xFF8C8C8C)
                                )
                            }
                        } else {
                            OfficialNoiseSlider(
                                gears = ancGears,
                                currentGear = noiseState.ancLevelIndex,
                                showDots = true,
                                showLevelDes = true,
                                levelDesText = depthDescription,
                                lowIconRes = "drawable/device_settings_reduct_low.webp",
                                highIconRes = "drawable/device_settings_reduct_high.webp",
                                enabled = !noiseState.isAutoNoise,
                                onLevelSelected = onAncLevelChange
                            )
                        }
                    }
                }

                // 2c. personalized_noise_reduction_switch (Config 59, line 150)
                // In official XML: placed at the bottom below levelContainer with marginTop 20dp
                if (capabilities.hasPersonalizedAnc) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPersonalizedAncChange(!noiseState.isPersonalizedAnc) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringRes("device_settings_personalized_noise_reduction"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Switch(
                            checked = noiseState.isPersonalizedAnc,
                            onCheckedChange = onPersonalizedAncChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF007AFF)
                            )
                        )
                    }
                }
            }
        }

        // 3. Sub-controls for Transparency Mode (if supported)
        if (!capabilities.isSingleToggleOnly) {
            AnimatedVisibility(
                visible = noiseState.mode == NoiseMode.TRANSPARENCY,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                    HorizontalDivider(
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val tLevels = capabilities.transparencyLevels.ifEmpty { listOf(0, 1, 2) }
                    if (tLevels.size > 1) {
                        val transDescription = when (noiseState.transparencyLevel) {
                            TransparencyLevel.REGULAR -> stringRes("device_settings_noise_reduction_transparent_standard")
                            TransparencyLevel.VOCAL -> stringRes("device_settings_noise_reduction_transparent_person")
                            TransparencyLevel.AMBIENT -> stringRes("device_settings_noise_reduction_transparent_environment")
                            TransparencyLevel.VOCAL_PLUS -> stringRes("device_settings_noise_reduction_transparent_person_plus")
                        }
                        OfficialNoiseSlider(
                            gears = tLevels,
                            currentGear = noiseState.transparencyLevelIndex,
                            showDots = true,
                            showLevelDes = true,
                            levelDesText = transDescription,
                            lowIconRes = "drawable/device_settings_trans_low.webp",
                            highIconRes = "drawable/device_settings_trans_high.webp",
                            enabled = true,
                            onLevelSelected = onTransparencyLevelChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixNoiseRadioItem(
    label: String,
    isSelected: Boolean,
    normalDrawable: String,
    checkedDrawable: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(if (isSelected) checkedDrawable else normalDrawable),
                contentDescription = label,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 1:1 official SeekBar + LevelDotView.
 * - Pill capsule track: height 23dp (device_settings_bg_noise_reduction_seekbar), corners 11.5dp.
 * - Thumb: device_settings_ic_noise_reduction_btn.webp (size 22dp), centered vertically inside track.
 * - Discrete dots (#8C8C8C, radius 4dp, paddingHor 8dp) for <= 9 gears, current dot skipped.
 * - Continuous smooth slider for > 9 gears.
 * - Disabled interactive gestures when enabled == false (e.g. Adaptive ANC active), while thumb still moves with live ambient data.
 */
@Composable
private fun OfficialNoiseSlider(
    gears: List<Int>,
    currentGear: Int,
    showDots: Boolean,
    showLevelDes: Boolean,
    levelDesText: String,
    lowIconRes: String,
    highIconRes: String,
    enabled: Boolean,
    onLevelSelected: (Int) -> Unit
) {
    val count = gears.size
    val selectedPos = gears.indexOf(currentGear).let { if (it >= 0) it else currentGear.coerceIn(0, (count - 1).coerceAtLeast(0)) }
    val pos = selectedPos.coerceIn(0, (count - 1).coerceAtLeast(0))
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val trackColor = if (isDark) Color(0xFF2F2E32) else Color(0xFFE2E2E6)
    val contentAlpha = if (enabled) 1f else 0.45f

    Column(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = contentAlpha }) {
        if (showLevelDes) {
            Text(
                text = levelDesText,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(lowIconRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))

            var dragPos by remember(pos, count) { mutableStateOf(pos.toFloat()) }
            var dragging by remember { mutableStateOf(false) }
            val displayPos = if (dragging) dragPos else pos.toFloat()

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .pointerInput(count, enabled) {
                        if (!enabled || count < 2) return@pointerInput
                        detectTapGestures { offset ->
                            val thumbRadiusPx = 11.dp.toPx()
                            val usableWidthPx = (size.width - 2 * thumbRadiusPx).coerceAtLeast(1f)
                            val touchX = (offset.x - thumbRadiusPx).coerceIn(0f, usableWidthPx)
                            val f = touchX / usableWidthPx
                            val targetIndex = (f * (count - 1)).roundToInt().coerceIn(0, count - 1)
                            onLevelSelected(gears[targetIndex])
                        }
                    }
                    .pointerInput(count, enabled) {
                        if (!enabled || count < 2) return@pointerInput
                        detectDragGestures(
                            onDragStart = { dragging = true },
                            onDragEnd = {
                                dragging = false
                                val targetIndex = dragPos.roundToInt().coerceIn(0, count - 1)
                                onLevelSelected(gears[targetIndex])
                            },
                            onDragCancel = { dragging = false }
                        ) { change, _ ->
                            val thumbRadiusPx = 11.dp.toPx()
                            val usableWidthPx = (size.width - 2 * thumbRadiusPx).coerceAtLeast(1f)
                            val touchX = (change.position.x - thumbRadiusPx).coerceIn(0f, usableWidthPx)
                            val f = touchX / usableWidthPx
                            dragPos = f * (count - 1)
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                val thumbSizeDp = 22.dp
                val trackHeightDp = 23.dp

                // Track pill (device_settings_bg_noise_reduction_seekbar)
                Canvas(modifier = Modifier.fillMaxWidth().height(trackHeightDp)) {
                    drawRoundRect(
                        color = trackColor,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                    )
                }

                // LevelDotView dots (when count <= 9)
                if (showDots && count > 1) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(trackHeightDp)) {
                        val dotRadiusPx = 4.dp.toPx()
                        val padPx = 8.dp.toPx()
                        val usableDotWidth = size.width - 2 * padPx
                        val cy = size.height / 2f
                        val skip = displayPos.roundToInt().coerceIn(0, count - 1)
                        for (i in 0 until count) {
                            if (i == skip) continue
                            val cx = padPx + (usableDotWidth * i / (count - 1))
                            drawCircle(
                                color = Color(0xFF8C8C8C),
                                radius = dotRadiusPx,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }

                // Official thumb (device_settings_ic_noise_reduction_btn)
                if (count > 1) {
                    val f = (displayPos / (count - 1)).coerceIn(0f, 1f)
                    val usableWidthDp = (maxWidth - thumbSizeDp).coerceAtLeast(0.dp)
                    val thumbOffsetXDp = usableWidthDp * f

                    Image(
                        painter = painterResource("drawable/device_settings_ic_noise_reduction_btn.webp"),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = thumbOffsetXDp)
                            .size(thumbSizeDp)
                            .align(Alignment.CenterStart)
                    )
                }
            }

            Spacer(modifier = Modifier.width(7.dp))
            Image(
                painter = painterResource(highIconRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
