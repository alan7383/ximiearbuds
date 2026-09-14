package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.core.protocol.EarbudGestures
import com.alan.ximiearbuds.core.protocol.GestureAction
import com.alan.ximiearbuds.core.protocol.GestureSettings
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 Faithful Reproduction of official Gesture Settings (device_settings_fragment_gesture.xml)
 */
@Composable
fun XiaomiGestureDialog(
    gestureSettings: GestureSettings,
    activeModel: EarbudsModel?,
    onUpdateGestures: (GestureSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedEar by remember { mutableStateOf(0) } // 0 = Left, 1 = Right
    var editingGestureType by remember { mutableStateOf<String?>(null) } // "single", "double", "triple", "long", "slide"

    val currentEarGestures = if (selectedEar == 0) gestureSettings.left else gestureSettings.right

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(520.dp)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            color = XiaomiPageBg,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = XiaomiOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringRes("device_settings_gesture_operation"),
                            color = XiaomiTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

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

                Spacer(modifier = Modifier.height(16.dp))

                // Earbud Tab Selector (Left / Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF222226))
                        .padding(4.dp)
                ) {
                    val ears = listOf(
                        0 to stringRes("device_settings_left"),
                        1 to stringRes("device_settings_right")
                    )

                    for ((idx, label) in ears) {
                        val isSelected = selectedEar == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) XiaomiOrange else Color.Transparent)
                                .clickable { selectedEar = idx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else XiaomiTextSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gesture List for Selected Ear
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    XiaomiCardContainer {
                        // 1. Single Press
                        val singleTitle = if (selectedEar == 0) "device_settings_left_click" else "device_settings_right_click"
                        XiaomiActionItem(
                            title = stringRes(singleTitle),
                            badgeText = stringRes(currentEarGestures.singleTap.stringKey),
                            onClick = { editingGestureType = "single" }
                        )

                        // 2. Double Press
                        val doubleTitle = if (selectedEar == 0) "device_settings_left_double_click" else "device_settings_right_double_click"
                        XiaomiActionItem(
                            title = stringRes(doubleTitle),
                            badgeText = stringRes(currentEarGestures.doubleTap.stringKey),
                            onClick = { editingGestureType = "double" }
                        )

                        // 3. Triple Press
                        val tripleTitle = if (selectedEar == 0) "device_settings_left_triple_click" else "device_settings_right_triple_click"
                        XiaomiActionItem(
                            title = stringRes(tripleTitle),
                            badgeText = stringRes(currentEarGestures.tripleTap.stringKey),
                            onClick = { editingGestureType = "triple" }
                        )

                        // 4. Long Press
                        val longTitle = if (selectedEar == 0) "device_settings_left_long_press" else "device_settings_right_long_press"
                        XiaomiActionItem(
                            title = stringRes(longTitle),
                            badgeText = stringRes(currentEarGestures.longPress.stringKey),
                            onClick = { editingGestureType = "long" }
                        )

                        // 5. Slide (if model supports slide gesture)
                        if (activeModel?.hasSlideGesture == true) {
                            val slideTitle = if (selectedEar == 0) "device_settings_left_slide" else "device_settings_right_slide"
                            XiaomiActionItem(
                                title = stringRes(slideTitle),
                                badgeText = stringRes(currentEarGestures.slide.stringKey),
                                onClick = { editingGestureType = "slide" }
                            )
                        }
                    }
                }
            }
        }
    }

    // Action Selection Dialog
    if (editingGestureType != null) {
        val gType = editingGestureType!!
        val currentAction = when (gType) {
            "single" -> currentEarGestures.singleTap
            "double" -> currentEarGestures.doubleTap
            "triple" -> currentEarGestures.tripleTap
            "long" -> currentEarGestures.longPress
            "slide" -> currentEarGestures.slide
            else -> GestureAction.NONE
        }

        val availableActions = if (gType == "slide") {
            listOf(
                GestureAction.VOLUME_ADJUST,
                GestureAction.PREVIOUS_NEXT,
                GestureAction.VOLUME_UP,
                GestureAction.VOLUME_DOWN,
                GestureAction.NONE
            )
        } else {
            listOf(
                GestureAction.PLAY_PAUSE,
                GestureAction.NEXT_TRACK,
                GestureAction.PREV_TRACK,
                GestureAction.NOISE_CONTROL,
                GestureAction.VOICE_ASSISTANT,
                GestureAction.VOLUME_UP,
                GestureAction.VOLUME_DOWN,
                GestureAction.QUICK_PHOTO,
                GestureAction.LONG_PRESS_RECORD,
                GestureAction.LONG_PRESS_TRANSLATE,
                GestureAction.NONE
            )
        }

        Dialog(onDismissRequest = { editingGestureType = null }) {
            Surface(
                modifier = Modifier
                    .width(460.dp)
                    .fillMaxHeight(0.75f)
                    .clip(RoundedCornerShape(20.dp)),
                color = XiaomiCardBg,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringRes("device_settings_gesture_operation"),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = XiaomiTextPrimary
                        )
                        IconButton(
                            onClick = { editingGestureType = null },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = XiaomiTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (action in availableActions) {
                            val isSelected = currentAction == action
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) XiaomiOrange.copy(alpha = 0.15f) else Color(0xFF222226))
                                    .clickable {
                                        val updatedGestures = when (gType) {
                                            "single" -> currentEarGestures.copy(singleTap = action)
                                            "double" -> currentEarGestures.copy(doubleTap = action)
                                            "triple" -> currentEarGestures.copy(tripleTap = action)
                                            "long" -> currentEarGestures.copy(longPress = action)
                                            "slide" -> currentEarGestures.copy(slide = action)
                                            else -> currentEarGestures
                                        }

                                        val newSettings = if (selectedEar == 0) {
                                            gestureSettings.copy(left = updatedGestures)
                                        } else {
                                            gestureSettings.copy(right = updatedGestures)
                                        }

                                        onUpdateGestures(newSettings)
                                        editingGestureType = null
                                    }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringRes(action.stringKey),
                                    color = if (isSelected) XiaomiOrange else XiaomiTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(XiaomiOrange)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
