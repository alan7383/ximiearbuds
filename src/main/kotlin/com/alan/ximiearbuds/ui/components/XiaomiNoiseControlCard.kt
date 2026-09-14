package com.alan.ximiearbuds.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.device.DeviceRegistry
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.core.protocol.AncLevel
import com.alan.ximiearbuds.core.protocol.NoiseControlState
import com.alan.ximiearbuds.core.protocol.NoiseMode
import com.alan.ximiearbuds.core.protocol.TransparencyLevel
import com.alan.ximiearbuds.ui.theme.*
import kotlin.math.roundToInt

/**
 * 1:1 Compose implementation of official Xiaomi Earbuds NoiseReductionView and
 * device_settings_layout_noise_redution.xml.
 *
 * Dynamically adapts to each model's specific capabilities:
 * - Single switch toggle for bone conduction / models without transparency (noTrans() == true)
 * - 3 Radio buttons (Transparence, Annulation du bruit, Désactivé)
 * - Smart Denoise switch (Function 1008)
 * - Adaptive Noise Cancellation switch (auto_fit / auto_button)
 * - Stepped Dot Slider (LevelDotView) with mild/balanced/deep/wind labels and low/high icons
 * - Personalized Noise Reduction switch (Function 3012, e.g. Redmi Buds 6 Pro / 5 Pro)
 */
@Composable
fun XiaomiNoiseControlCard(
    noiseState: NoiseControlState,
    model: EarbudsModel,
    onModeChange: (NoiseMode) -> Unit,
    onAncLevelIndexChange: (index: Int, rawLevel: Int) -> Unit,
    onTransparencyLevelIndexChange: (index: Int, rawLevel: Int) -> Unit,
    onAutoNoiseChange: (Boolean) -> Unit = {},
    onSmartDenoiseChange: (Boolean) -> Unit = {},
    onPersonalizedAncChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val caps = model.ancCapabilities
    val isSingleToggle = caps.isSingleToggleOnly || model.isBoneConduction || !model.hasTransparency

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(XiaomiCardBg)
            .border(1.dp, XiaomiCardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        if (isSingleToggle) {
            // Case 1: Models without transparency / Bone conduction (reduction_switch)
            val isAncOn = noiseState.mode == NoiseMode.ANC
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringRes("device_settings_noise_reduction"),
                    color = XiaomiTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Switch(
                    checked = isAncOn,
                    onCheckedChange = { checked ->
                        onModeChange(if (checked) NoiseMode.ANC else NoiseMode.OFF)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = XiaomiCyan,
                        uncheckedThumbColor = Color(0xFFAAAAAA),
                        uncheckedTrackColor = Color(0xFF333336),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
        } else {
            // Case 2: Standard TWS earbuds with 3-mode RadioGroup (noise_reduction_group)
            // Exact order from device_settings_layout_noise_redution.xml:
            // 1: noise_reduction_transparent
            // 2: noise_reduction_open
            // 3: noise_reduction_close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // 1. Transparency ("Transparence")
                MiuiNoiseRadioButton(
                    label = stringRes("device_settings_noise_reduction_transparent"),
                    normalIcon = "drawable/device_settings_trans.webp",
                    checkedIcon = "drawable/device_settings_trans_checked.webp",
                    isSelected = noiseState.mode == NoiseMode.TRANSPARENCY,
                    onClick = { onModeChange(NoiseMode.TRANSPARENCY) },
                    modifier = Modifier.weight(1f)
                )

                // 2. Noise reduction ("Annulation du bruit")
                MiuiNoiseRadioButton(
                    label = stringRes("device_settings_noise_reduction_open"),
                    normalIcon = "drawable/device_settings_noise.webp",
                    checkedIcon = "drawable/device_settings_noise_checked.webp",
                    isSelected = noiseState.mode == NoiseMode.ANC,
                    onClick = { onModeChange(NoiseMode.ANC) },
                    modifier = Modifier.weight(1f)
                )

                // 3. Off ("Désactivé")
                MiuiNoiseRadioButton(
                    label = stringRes("device_settings_noise_reduction_close"),
                    normalIcon = "drawable/device_settings_noise_close.webp",
                    checkedIcon = "drawable/device_settings_noise_close_checked.webp",
                    isSelected = noiseState.mode == NoiseMode.OFF,
                    onClick = { onModeChange(NoiseMode.OFF) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Sub-controls container when ANC is active
        AnimatedVisibility(
            visible = noiseState.mode == NoiseMode.ANC,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))
                MiuiDivider()

                // Smart Denoise switch (function 1008, e.g. REDMI Buds 8 Pro P76)
                if (caps.hasSmartDenoise) {
                    MiuiSwitchRow(
                        title = stringRes("device_settings_smart_denoise"),
                        checked = noiseState.isSmartDenoise,
                        onCheckedChange = onSmartDenoiseChange
                    )
                    MiuiDivider()
                }

                // Level container
                val ancGears = caps.ancLevels
                val currentAncIndex = remember(noiseState.ancLevel, noiseState.ancLevelIndex, ancGears) {
                    val idx = ancGears.indexOf(noiseState.ancLevel.id)
                    if (idx >= 0) idx else noiseState.ancLevelIndex.coerceIn(0, (ancGears.size - 1).coerceAtLeast(0))
                }

                val ancLevelLabel = when (ancGears.getOrNull(currentAncIndex)) {
                    1 -> stringRes("device_settings_mild")
                    0 -> stringRes("device_settings_balanced")
                    2 -> stringRes("device_settings_deep")
                    4 -> stringRes("device_settings_wind_resistance")
                    else -> stringRes("device_settings_balanced")
                }

                // Adaptive Noise Reduction switch (auto_fit / auto_button, e.g. Redmi Buds 6 Pro O76)
                if (caps.hasAdaptiveAnc) {
                    MiuiSwitchRow(
                        title = stringRes("device_settings_noise_reduction_adaptive"),
                        checked = noiseState.isAutoNoise,
                        onCheckedChange = onAutoNoiseChange
                    )
                }

                if (ancGears.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // levelDesView: centered current level text
                    Text(
                        text = ancLevelLabel,
                        color = if (noiseState.isAutoNoise) XiaomiTextSecondary else XiaomiTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stepped seekbar with LevelDotView
                    MiuiSteppedDotSlider(
                        count = ancGears.size,
                        currentIndex = currentAncIndex,
                        enabled = !noiseState.isAutoNoise,
                        lowIconRes = "drawable/device_settings_reduct_low.webp",
                        highIconRes = "drawable/device_settings_reduct_high.webp",
                        onIndexSelected = { newIdx ->
                            val rawGear = ancGears.getOrElse(newIdx) { 0 }
                            onAncLevelIndexChange(newIdx, rawGear)
                        }
                    )
                }

                // Personalized Noise Reduction switch (function 3012, e.g. Redmi Buds 6 Pro O76)
                if (caps.hasPersonalizedAnc) {
                    Spacer(modifier = Modifier.height(14.dp))
                    MiuiDivider()
                    MiuiSwitchRow(
                        title = stringRes("device_settings_personalized_noise_reduction"),
                        checked = noiseState.isPersonalizedAnc,
                        onCheckedChange = onPersonalizedAncChange
                    )
                }
            }
        }

        // Sub-controls container when Transparency is active
        AnimatedVisibility(
            visible = noiseState.mode == NoiseMode.TRANSPARENCY,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val transGears = caps.transparencyLevels
            val currentTransIndex = remember(noiseState.transparencyLevel, noiseState.transparencyLevelIndex, transGears) {
                val idx = transGears.indexOf(noiseState.transparencyLevel.id)
                if (idx >= 0) idx else noiseState.transparencyLevelIndex.coerceIn(0, (transGears.size - 1).coerceAtLeast(0))
            }

            val transLevelLabel = when (transGears.getOrNull(currentTransIndex)) {
                0 -> stringRes("device_settings_noise_reduction_transparent_standard")
                1 -> stringRes("device_settings_noise_reduction_transparent_person")
                2 -> stringRes("device_settings_noise_reduction_transparent_environment")
                3 -> stringRes("device_settings_noise_reduction_transparent_person_plus")
                else -> stringRes("device_settings_noise_reduction_transparent_standard")
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))
                MiuiDivider()

                if (transGears.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // levelDesView: centered transparency mode label
                    Text(
                        text = transLevelLabel,
                        color = XiaomiTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stepped seekbar for transparency
                    MiuiSteppedDotSlider(
                        count = transGears.size,
                        currentIndex = currentTransIndex,
                        enabled = true,
                        lowIconRes = "drawable/device_settings_trans_low.webp",
                        highIconRes = "drawable/device_settings_trans_high.webp",
                        onIndexSelected = { newIdx ->
                            val rawGear = transGears.getOrElse(newIdx) { 0 }
                            onTransparencyLevelIndexChange(newIdx, rawGear)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Backward compatibility overload for existing callers/tests.
 */
@Composable
fun XiaomiNoiseControlCard(
    noiseState: NoiseControlState,
    onModeChange: (NoiseMode) -> Unit,
    onAncLevelChange: (AncLevel) -> Unit,
    onTransparencyLevelChange: (TransparencyLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    XiaomiNoiseControlCard(
        noiseState = noiseState,
        model = DeviceRegistry.GENERIC_MODEL,
        onModeChange = onModeChange,
        onAncLevelIndexChange = { _, raw -> onAncLevelChange(AncLevel.fromId(raw)) },
        onTransparencyLevelIndexChange = { _, raw -> onTransparencyLevelChange(TransparencyLevel.fromId(raw)) },
        modifier = modifier
    )
}

/**
 * Radio button replicating DeviceSettingsNoiseReductionRadioBtn style and behavior.
 */
@Composable
private fun MiuiNoiseRadioButton(
    label: String,
    normalIcon: String,
    checkedIcon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(if (isSelected) checkedIcon else normalIcon),
            contentDescription = label,
            modifier = Modifier.size(52.dp)
        )

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = label,
            color = if (isSelected) XiaomiTextPrimary else XiaomiTextSecondary,
            fontSize = 13.5.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 16.sp
        )
    }
}

/**
 * Reusable single-line switch item replicating SwitchButtonBindingSingleLineTextView.
 */
@Composable
private fun MiuiSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = XiaomiTextPrimary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = XiaomiCyan,
                uncheckedThumbColor = Color(0xFFAAAAAA),
                uncheckedTrackColor = Color(0xFF333336),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

/**
 * Stepped dot seekbar replicating LevelDotView + SeekBar from official MIUI app.
 */
@Composable
private fun MiuiSteppedDotSlider(
    count: Int,
    currentIndex: Int,
    enabled: Boolean,
    lowIconRes: String,
    highIconRes: String,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (enabled) 1.0f else 0.38f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left icon (low)
        Image(
            painter = painterResource(lowIconRes),
            contentDescription = "Low",
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Center stepped slider
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .pointerInput(enabled, count) {
                    if (!enabled || count <= 1) return@pointerInput
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val targetIdx = (fraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
                        onIndexSelected(targetIdx)
                    }
                }
                .pointerInput(enabled, count) {
                    if (!enabled || count <= 1) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        val targetIdx = (fraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
                        if (targetIdx != currentIndex) {
                            onIndexSelected(targetIdx)
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidth = maxWidth
            val safeCount = count.coerceAtLeast(2)

            // Background Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF2C2C30))
            )

            // Level Dots along the track
            for (i in 0 until count) {
                val fraction = i.toFloat() / (safeCount - 1)
                val dotOffset = totalWidth * fraction

                if (i != currentIndex) {
                    Box(
                        modifier = Modifier
                            .offset(x = dotOffset - 3.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8C8C94))
                    )
                }
            }

            // Active Thumb
            val thumbFraction = currentIndex.coerceIn(0, safeCount - 1).toFloat() / (safeCount - 1)
            val thumbOffset = totalWidth * thumbFraction

            Box(
                modifier = Modifier
                    .offset(x = thumbOffset - 11.dp)
                    .size(22.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.5.dp, Color(0xFFD4D4D8), CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right icon (high)
        Image(
            painter = painterResource(highIconRes),
            contentDescription = "High",
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun MiuiDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(XiaomiDivider)
    )
}
