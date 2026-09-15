package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.core.protocol.EarbudGestures
import com.alan.ximiearbuds.core.protocol.GestureAction
import com.alan.ximiearbuds.core.protocol.GestureSettings
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiActionItem
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.theme.stringRes

/**
 * 1:1 replica of Xiaomi Earbuds `device_settings_fragment_gesture.xml`.
 * 
 * Replaces popup dialog with an authentic full-screen MIUI fragment:
 * - Top navigation bar with back chevron
 * - Segmented tabs for Left Earbud and Right Earbud
 * - Tap triggers: Single tap, Double tap, Triple tap, Long press, Stem slide
 * - Modal bottom sheet or action selector for gesture mapping
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiuixGestureScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gestureSettings by controller.gestures.collectAsState()
    var selectedEar by remember { mutableStateOf(0) } // 0 = Left, 1 = Right
    var editingGestureType by remember { mutableStateOf<String?>(null) } // "single", "double", "triple", "long", "slide"

    val currentGestures = if (selectedEar == 0) gestureSettings.left else gestureSettings.right
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top MIUI Navigation Bar
        MiuixTopAppBar(
            title = stringRes("device_settings_gesture_operation"),
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Tab for Left / Right Earbud
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedEar == 0) Color(0xFF007AFF) else Color.Transparent)
                            .clickable { selectedEar = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringRes("device_settings_find_side_left"),
                            color = if (selectedEar == 0) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = if (selectedEar == 0) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedEar == 1) Color(0xFF007AFF) else Color.Transparent)
                            .clickable { selectedEar = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringRes("device_settings_find_side_right"),
                            color = if (selectedEar == 1) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = if (selectedEar == 1) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section Header: Actions au toucher
            Text(
                text = stringRes("device_settings_gesture_operation").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            // Card Group with all gestures
            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                // Single Tap
                XiaomiActionItem(
                    title = stringRes("device_settings_once_click_mbf"),
                    subtitle = formatGestureAction(currentGestures.singleTap),
                    onClick = { editingGestureType = "single" }
                )

                XiaomiItemDivider()

                // Double Tap
                XiaomiActionItem(
                    title = stringRes("device_settings_double_click_mbf"),
                    subtitle = formatGestureAction(currentGestures.doubleTap),
                    onClick = { editingGestureType = "double" }
                )

                XiaomiItemDivider()

                // Triple Tap
                XiaomiActionItem(
                    title = stringRes("device_settings_triple_strike_mbf"),
                    subtitle = formatGestureAction(currentGestures.tripleTap),
                    onClick = { editingGestureType = "triple" }
                )

                XiaomiItemDivider()

                // Long Press
                XiaomiActionItem(
                    title = stringRes("device_settings_long_press_mbf_two_seconds"),
                    subtitle = formatGestureAction(currentGestures.longPress),
                    onClick = { editingGestureType = "long" }
                )

                XiaomiItemDivider()

                // Slide / Stem gesture
                XiaomiActionItem(
                    title = stringRes("device_settings_volume_changed"),
                    subtitle = formatGestureAction(currentGestures.slide),
                    onClick = { editingGestureType = "slide" }
                )
            }
        }
    }

    // Modal Bottom Sheet to choose gesture action
    editingGestureType?.let { gestureType ->
        ModalBottomSheet(
            onDismissRequest = { editingGestureType = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                val actionTitle = when (gestureType) {
                    "single" -> stringRes("device_settings_once_click_mbf")
                    "double" -> stringRes("device_settings_double_click_mbf")
                    "triple" -> stringRes("device_settings_triple_strike_mbf")
                    "long" -> stringRes("device_settings_long_press_mbf_two_seconds")
                    else -> stringRes("device_settings_volume_changed")
                }

                Text(
                    text = actionTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val actions = listOf(
                    GestureAction.PLAY_PAUSE to stringRes("device_settings_play_or_pause"),
                    GestureAction.NEXT_TRACK to stringRes("device_settings_next_song"),
                    GestureAction.PREV_TRACK to stringRes("device_settings_last_song"),
                    GestureAction.NOISE_CONTROL to stringRes("device_settings_noise_control"),
                    GestureAction.VOLUME_UP to stringRes("device_settings_volume_up"),
                    GestureAction.VOLUME_DOWN to stringRes("device_settings_volume_down"),
                    GestureAction.VOICE_ASSISTANT to stringRes("device_settings_awake_voice_assistant"),
                    GestureAction.NONE to stringRes("device_settings_click_cancle")
                )

                val currentAction = when (gestureType) {
                    "single" -> currentGestures.singleTap
                    "double" -> currentGestures.doubleTap
                    "triple" -> currentGestures.tripleTap
                    "long" -> currentGestures.longPress
                    else -> currentGestures.slide
                }

                for ((act, label) in actions) {
                    val isCurrent = currentAction == act
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                updateEarbudGesture(
                                    controller = controller,
                                    isLeft = selectedEar == 0,
                                    type = gestureType,
                                    newAction = act
                                )
                                editingGestureType = null
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = if (isCurrent) Color(0xFF007AFF) else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp
                        )

                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun updateEarbudGesture(
    controller: EarbudsController,
    isLeft: Boolean,
    type: String,
    newAction: GestureAction
) {
    val currentSettings = controller.gestures.value
    val targetEar = if (isLeft) currentSettings.left else currentSettings.right
    val updatedEar = when (type) {
        "single" -> targetEar.copy(singleTap = newAction)
        "double" -> targetEar.copy(doubleTap = newAction)
        "triple" -> targetEar.copy(tripleTap = newAction)
        "long" -> targetEar.copy(longPress = newAction)
        "slide" -> targetEar.copy(slide = newAction)
        else -> targetEar
    }

    val updatedSettings = if (isLeft) {
        currentSettings.copy(left = updatedEar)
    } else {
        currentSettings.copy(right = updatedEar)
    }

    controller.updateGestures(updatedSettings)
}

@Composable
private fun formatGestureAction(action: GestureAction): String {
    return when (action) {
        GestureAction.PLAY_PAUSE -> stringRes("device_settings_play_or_pause")
        GestureAction.NEXT_TRACK -> stringRes("device_settings_next_song")
        GestureAction.PREV_TRACK -> stringRes("device_settings_last_song")
        GestureAction.NOISE_CONTROL -> stringRes("device_settings_noise_control")
        GestureAction.VOLUME_UP -> stringRes("device_settings_volume_up")
        GestureAction.VOLUME_DOWN -> stringRes("device_settings_volume_down")
        GestureAction.VOICE_ASSISTANT -> stringRes("device_settings_awake_voice_assistant")
        GestureAction.NONE -> stringRes("device_settings_click_cancle")
        else -> stringRes("device_settings_others")
    }
}
