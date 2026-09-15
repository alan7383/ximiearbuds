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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.protocol.AncLevel
import com.alan.ximiearbuds.core.protocol.NoiseControlState
import com.alan.ximiearbuds.ui.theme.stringRes
import com.alan.ximiearbuds.core.protocol.NoiseMode
import com.alan.ximiearbuds.core.protocol.TransparencyLevel

/**
 * Authentic 1:1 replica of Xiaomi Earbuds `device_settings_layout_noise_redution.xml`.
 * 
 * Features:
 * - 3 large radio mode selectors (Transparency, ANC, Off) using official WebP icons
 * - LevelDotView stepped dot seekbar with discrete stops
 * - Low & High level anchor icons (`device_settings_trans_low.webp`, `device_settings_trans_high.webp`)
 * - Adaptive Smart Denoise switch
 * - Transparency mode chips (Standard, Vocal Enhancement, Ambient)
 */
@Composable
fun MiuixNoiseReductionView(
    noiseState: NoiseControlState,
    onModeChange: (NoiseMode) -> Unit,
    onAncLevelChange: (AncLevel) -> Unit,
    onTransparencyLevelChange: (TransparencyLevel) -> Unit,
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
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        // Row of 3 Radio Buttons: Transparency, ANC, Off
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            // 1. Transparency (noise_reduction_transparent)
            MiuixNoiseRadioItem(
                label = stringRes("device_settings_notification_transparency_mode"),
                isSelected = noiseState.mode == NoiseMode.TRANSPARENCY,
                normalDrawable = "drawable/device_settings_trans.webp",
                checkedDrawable = "drawable/device_settings_trans_checked.webp",
                onClick = { onModeChange(NoiseMode.TRANSPARENCY) },
                modifier = Modifier.weight(1f)
            )

            // 2. Active Noise Cancellation (noise_reduction_open)
            MiuixNoiseRadioItem(
                label = stringRes("device_settings_noise_control"),
                isSelected = noiseState.mode == NoiseMode.ANC,
                normalDrawable = "drawable/device_settings_noise.webp",
                checkedDrawable = "drawable/device_settings_noise_checked.webp",
                onClick = { onModeChange(NoiseMode.ANC) },
                modifier = Modifier.weight(1f)
            )

            // 3. Off (noise_reduction_close)
            MiuixNoiseRadioItem(
                label = stringRes("device_settings_noise_close"),
                isSelected = noiseState.mode == NoiseMode.OFF,
                normalDrawable = "drawable/device_settings_noise_close.webp",
                checkedDrawable = "drawable/device_settings_noise_close_checked.webp",
                onClick = { onModeChange(NoiseMode.OFF) },
                modifier = Modifier.weight(1f)
            )
        }

        // Sub-controls for Active Noise Cancellation
        AnimatedVisibility(
            visible = noiseState.mode == NoiseMode.ANC,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                HorizontalDivider(
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Adaptive Denoise Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSmartDenoiseChange(!noiseState.isSmartDenoise) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringRes("device_settings_smart_denoise"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringRes("device_settings_noise_reduction_adaptive_noise"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }

                    Switch(
                        checked = noiseState.isSmartDenoise,
                        onCheckedChange = onSmartDenoiseChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF007AFF)
                        )
                    )
                }

                // LevelDotView Stepped Slider (active if not smart denoise)
                if (!noiseState.isSmartDenoise) {
                    Spacer(modifier = Modifier.height(14.dp))

                    val depthDescription = when (noiseState.ancLevel) {
                        AncLevel.LIGHT -> "Réduction de bruit légère"
                        AncLevel.BALANCED -> "Réduction de bruit équilibrée"
                        AncLevel.DEEP, AncLevel.DEEP_PLUS -> "Réduction de bruit profonde"
                        AncLevel.ADAPTIVE -> "Réduction adaptative"
                        AncLevel.ANTIWIND -> "Anti-vent"
                    }

                    Text(
                        text = depthDescription,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF007AFF),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stepped Dot Track
                    MiuixLevelDotView(
                        currentLevel = noiseState.ancLevel,
                        onLevelSelected = onAncLevelChange
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringRes("device_settings_mild"),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                        Text(
                            text = stringRes("device_settings_deep"),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }

        // Sub-controls for Transparency Mode
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringRes("device_settings_notification_transparency_mode"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiuixProfileChip(
                        label = stringRes("device_settings_scene_standard"),
                        selected = noiseState.transparencyLevel == TransparencyLevel.REGULAR,
                        onClick = { onTransparencyLevelChange(TransparencyLevel.REGULAR) },
                        modifier = Modifier.weight(1f)
                    )
                    MiuixProfileChip(
                        label = stringRes("device_settings_noise_reduction_transparent_person"),
                        selected = noiseState.transparencyLevel == TransparencyLevel.VOCAL,
                        onClick = { onTransparencyLevelChange(TransparencyLevel.VOCAL) },
                        modifier = Modifier.weight(1f)
                    )
                    MiuixProfileChip(
                        label = stringRes("device_settings_noise_reduction_transparent_environment"),
                        selected = noiseState.transparencyLevel == TransparencyLevel.AMBIENT,
                        onClick = { onTransparencyLevelChange(TransparencyLevel.AMBIENT) },
                        modifier = Modifier.weight(1f)
                    )
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
                modifier = Modifier.size(if (isSelected) 54.dp else 44.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.sp
            ),
            color = if (isSelected) Color(0xFF007AFF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 1:1 Compose replica of official MIUI `LevelDotView.java`.
 */
@Composable
private fun MiuixLevelDotView(
    currentLevel: AncLevel,
    onLevelSelected: (AncLevel) -> Unit
) {
    val levels = listOf(AncLevel.LIGHT, AncLevel.BALANCED, AncLevel.DEEP)
    val selectedIndex = when (currentLevel) {
        AncLevel.LIGHT -> 0
        AncLevel.BALANCED -> 1
        else -> 2
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Low Level Icon
        Image(
            painter = painterResource("drawable/device_settings_trans_low.webp"),
            contentDescription = "Faible",
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Stepped Track with Dots and Sliding Knob
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val trackWidth = size.width
                        val fraction = (offset.x / trackWidth).coerceIn(0f, 1f)
                        val stepIndex = when {
                            fraction < 0.33f -> 0
                            fraction < 0.67f -> 1
                            else -> 2
                        }
                        onLevelSelected(levels[stepIndex])
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Track bar
            Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                drawRoundRect(
                    color = Color(0x207F7F7F),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
            }

            // Dots along track
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                levels.forEachIndexed { index, _ ->
                    val isChecked = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .size(if (isChecked) 18.dp else 12.dp)
                            .clip(CircleShape)
                            .background(if (isChecked) Color(0xFF007AFF) else Color(0x407F7F7F)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // High Level Icon
        Image(
            painter = painterResource("drawable/device_settings_trans_high.webp"),
            contentDescription = "Fort",
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun MiuixProfileChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val bg = if (selected) Color(0xFF007AFF) else if (isDark) Color(0xFF2A2A2E) else Color(0xFFF2F3F5)
    val textCol = if (selected) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.sp
            ),
            color = textCol
        )
    }
}
