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
import androidx.compose.foundation.shape.CircleShape
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
 * Features:
 * - 3 large radio mode selectors (Transparency, ANC, Off) using official WebP icons
 * - Adaptively hides Transparency if single-toggle or bone conduction
 * - LevelDotView stepped dot seekbar with discrete stops matching model's tws_gear
 * - Adaptive Smart Denoise switch (1008)
 * - Personalized ANC switch (3012)
 * - Transparency mode chips (Standard, Vocal Enhancement, Ambient) based on model's tws_gear
 */
@Composable
fun MiuixNoiseReductionView(
    noiseState: NoiseControlState,
    capabilities: AncCapabilities = AncCapabilities(),
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
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .padding(vertical = 20.dp, horizontal = 16.dp)
    ) {
        // Row of Radio Buttons: Transparency (if supported), ANC, Off
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            // 1. Transparency (only if device supports transparency)
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

            // 2. Active Noise Cancellation (noise_reduction_open)
            MiuixNoiseRadioItem(
                label = stringRes("device_settings_noise_reduction_open"),
                isSelected = noiseState.mode == NoiseMode.ANC,
                normalDrawable = "drawable/device_settings_noise.webp",
                checkedDrawable = "drawable/device_settings_noise_checked.webp",
                onClick = { onModeChange(NoiseMode.ANC) },
                modifier = Modifier.weight(1f)
            )

            // 3. Off (noise_reduction_close)
            MiuixNoiseRadioItem(
                label = stringRes("device_settings_noise_reduction_close"),
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

                // 1:1 switch_btn_smart_denoise : titre seul, pas de sous-titre.
                if (capabilities.hasSmartDenoise || capabilities.hasAdaptiveAnc) {
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

                // 1:1 personalized_noise_reduction_switch : titre seul, pas de sous-titre.
                if (capabilities.hasPersonalizedAnc) {
                    Spacer(modifier = Modifier.height(10.dp))
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

                // 1:1 levelContainer : 1 gear -> rien ; >9 gears -> slider continu + mild/deep ;
                // sinon slider à crans + levelDes (référentiels reductionStr).
                val ancGears = capabilities.ancLevels.ifEmpty { listOf(1, 0, 2) }
                if (ancGears.size > 1) {
                    Spacer(modifier = Modifier.height(14.dp))

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
                            selectedPos = ancGears.indexOf(noiseState.ancLevel.id).coerceAtLeast(0),
                            showDots = false,
                            showLevelDes = false,
                            levelDesText = depthDescription,
                            lowIconRes = "drawable/device_settings_reduct_low.webp",
                            highIconRes = "drawable/device_settings_reduct_high.webp",
                            enabled = !noiseState.isSmartDenoise,
                            onLevelSelected = { onAncLevelChange(AncLevel.fromId(it)) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
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
                            selectedPos = ancGears.indexOf(noiseState.ancLevel.id).coerceAtLeast(0),
                            showDots = true,
                            showLevelDes = true,
                            levelDesText = depthDescription,
                            lowIconRes = "drawable/device_settings_reduct_low.webp",
                            highIconRes = "drawable/device_settings_reduct_high.webp",
                            enabled = !noiseState.isSmartDenoise,
                            onLevelSelected = { onAncLevelChange(AncLevel.fromId(it)) }
                        )
                    }
                }
            }
        }

        // Sub-controls for Transparency Mode (if supported)
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1:1 transparence : même slider à crans + levelDes (transStr),
                    // jamais de chips. 1 seul niveau -> rien (container masqué).
                    val tLevels = capabilities.transparencyLevels.ifEmpty { listOf(0, 1, 2) }
                    if (tLevels.size > 1) {
                        val transDescription = when (noiseState.transparencyLevel) {
                            TransparencyLevel.REGULAR -> stringRes("device_settings_noise_reduction_transparent_standard")
                            TransparencyLevel.VOCAL -> stringRes("device_settings_noise_reduction_transparent_person")
                            TransparencyLevel.AMBIENT -> stringRes("device_settings_noise_reduction_transparent_environment")
                        }
                        OfficialNoiseSlider(
                            gears = tLevels,
                            selectedPos = tLevels.indexOf(noiseState.transparencyLevel.id).coerceAtLeast(0),
                            showDots = true,
                            showLevelDes = true,
                            levelDesText = transDescription,
                            lowIconRes = "drawable/device_settings_trans_low.webp",
                            highIconRes = "drawable/device_settings_trans_high.webp",
                            enabled = true,
                            onLevelSelected = { onTransparencyLevelChange(TransparencyLevel.fromId(it)) }
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
                // 1:1 : même taille aux deux états (54dp = 162px @3x).
                // L'anneau bleu du checked est DANS l'asset, pas de saut de layout.
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
 * 1:1 réplique de NoiseReductionView + LevelDotView.java officiels.
 * - levelDes : 13sp text_color, centré (FontLatinMedium -> Medium).
 * - SeekBar : fond #2f2e32 coins 33dp, progress transparent,
 *   thumb = device_settings_ic_noise_reduction_btn.
 * - LevelDotView : N dots (N = max+1), rayon 4dp, #8C8C8C, paddingHor 8dp,
 *   dot sélectionné SAUTÉ (le thumb le recouvre) ; setData(max+1, progress).
 * - Tap + drag avec snap au cran ; commit au relâcher (onStopTrackingTouch).
 *
 * @param gears liste des niveaux bruts du modèle (ex. [1,0,2]), dans l'ordre.
 * @param selectedPos index courant dans [gears].
 * @param onLevelSelected appelé avec le niveau BRUT sélectionné.
 */
@Composable
private fun OfficialNoiseSlider(
    gears: List<Int>,
    selectedPos: Int,
    showDots: Boolean,
    showLevelDes: Boolean,
    levelDesText: String,
    lowIconRes: String,
    highIconRes: String,
    enabled: Boolean,
    onLevelSelected: (Int) -> Unit
) {
    val count = gears.size
    val pos = selectedPos.coerceIn(0, count - 1)
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val trackColor = if (isDark) Color(0xFF2F2E32) else Color(0xFFE2E2E6)
    val contentAlpha = if (enabled) 1f else 0.4f

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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .pointerInput(count, enabled) {
                        if (!enabled) return@pointerInput
                        detectTapGestures { offset ->
                            val f = (offset.x / size.width).coerceIn(0f, 1f)
                            onLevelSelected(gears[(f * (count - 1)).roundToInt().coerceIn(0, count - 1)])
                        }
                    }
                    .pointerInput(count, enabled) {
                        if (!enabled || count < 2) return@pointerInput
                        detectDragGestures(
                            onDragStart = { dragging = true },
                            onDragEnd = {
                                dragging = false
                                onLevelSelected(gears[dragPos.roundToInt().coerceIn(0, count - 1)])
                            },
                            onDragCancel = { dragging = false }
                        ) { change, _ ->
                            val f = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragPos = (f * (count - 1)).coerceIn(0f, (count - 1).toFloat())
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Fond seekbar : pill #2f2e32 (device_settings_bg_noise_reduction_seekbar).
                Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
                    drawRoundRect(
                        color = trackColor,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                    )
                }

                if (showDots && count > 1) {
                    // LevelDotView.onDraw : dots espacés sur (width - 2*paddingHor),
                    // rayon 4dp, #8C8C8C, dot courant sauté (recouvert par le thumb).
                    Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                        val dotR = 4.dp.toPx()
                        val pad = 8.dp.toPx()
                        val usable = size.width - pad * 2
                        val gap = if (count > 1) (usable - count * dotR * 2) / (count - 1) else 0f
                        val cy = size.height / 2
                        val skip = displayPos.roundToInt().coerceIn(0, count - 1)
                        for (i in 0 until count) {
                            if (i == skip) continue
                            val cx = pad + dotR + i * (dotR * 2 + gap)
                            drawCircle(color = Color(0xFF8C8C8C), radius = dotR, center = Offset(cx, cy))
                        }
                    }
                }

                // Thumb officiel (device_settings_ic_noise_reduction_btn) au cran courant.
                if (count > 1) {
                    val f = (displayPos / (count - 1)).coerceIn(0f, 1f)
                    Row(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                        if (f > 0f) Spacer(Modifier.weight(f).fillMaxHeight())
                        Image(
                            painter = painterResource("drawable/device_settings_ic_noise_reduction_btn.webp"),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp).align(Alignment.CenterVertically)
                        )
                        if (f < 1f) Spacer(Modifier.weight(1f - f).fillMaxHeight())
                    }
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
