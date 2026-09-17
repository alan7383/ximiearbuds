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
import com.alan.ximiearbuds.core.device.DeviceRegistry
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.core.protocol.EarbudGestures
import com.alan.ximiearbuds.core.protocol.GestureAction
import com.alan.ximiearbuds.core.protocol.GestureSettings
import com.alan.ximiearbuds.core.protocol.OfficialFunctionIds
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiActionItem
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.theme.stringRes

data class GestureEditTarget(
    val isLeft: Boolean,
    val gestureType: String,
    val funcId: Int,
    val titleKey: String
)

/**
 * 1:1 replica of Xiaomi Earbuds `device_settings_fragment_gesture.xml`.
 * 
 * Authentic single vertical layout displaying all trigger operations dynamically adapted
 * to each earbud model's physical gesture capabilities:
 * - Pinch gestures (Press once / Press twice / Press triple / Long press)
 * - Tap gestures (Double tap / Triple tap / Long press)
 * - Slide gestures (Slide up/down)
 * - Allowed actions dynamically filtered per model and gesture trigger
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiuixGestureScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeModel by controller.activeModel.collectAsState()
    val model = activeModel ?: DeviceRegistry.GENERIC_MODEL
    val gestureCaps = model.gestureCapabilities
    val gestureSettings by controller.gestures.collectAsState()
    var editingTarget by remember { mutableStateOf<GestureEditTarget?>(null) }
    val scrollState = rememberScrollState()

    val hasFunctions = model.supportedFunctionIds.isNotEmpty()
    val isPinch = gestureCaps.isPinchGesture || model.hasFunction(OfficialFunctionIds.FUNC_GESTURE_PRESS_TWICE) || model.hasFunction(OfficialFunctionIds.FUNC_DOUBLE_MFB)
    val showSinglePress = isPinch || model.hasFunction(OfficialFunctionIds.FUNC_GESTURE_PRESS_ONCE) || model.hasFunction(OfficialFunctionIds.FUNC_ONCE_MFB)
    val showDoubleClick = !hasFunctions || isPinch || model.hasFunction(OfficialFunctionIds.FUNC_GESTURE_DOUBLE_CLICK) || model.hasFunction(OfficialFunctionIds.FUNC_GESTURE_PRESS_TWICE) || model.hasFunction(OfficialFunctionIds.FUNC_DOUBLE_MFB)
    val showTripleClick = !hasFunctions || isPinch || model.hasFunction(OfficialFunctionIds.FUNC_GESTURE_TRIPLE_CLICK) || model.hasFunction(OfficialFunctionIds.FUNC_GESTURE_PRESS_TRIPLE) || model.hasFunction(OfficialFunctionIds.FUNC_TRIPLE_MFB)
    val showLongPress = !hasFunctions || model.hasFunction(OfficialFunctionIds.FUNC_GESTURE_LONG_PRESS) || model.hasFunction(OfficialFunctionIds.FUNC_LONG_PRESS_MFB)
    val showSlide = gestureCaps.isSlideGesture || model.hasFunction(OfficialFunctionIds.FUNC_GESTURE_SLIDE)

    // Function IDs for action filtering
    val singleFuncId = if (isPinch) OfficialFunctionIds.FUNC_GESTURE_PRESS_ONCE else OfficialFunctionIds.FUNC_GESTURE_PRESS_ONCE
    val doubleFuncId = if (isPinch) OfficialFunctionIds.FUNC_GESTURE_PRESS_TWICE else OfficialFunctionIds.FUNC_GESTURE_DOUBLE_CLICK
    val tripleFuncId = if (isPinch) OfficialFunctionIds.FUNC_GESTURE_PRESS_TRIPLE else OfficialFunctionIds.FUNC_GESTURE_TRIPLE_CLICK
    val longFuncId = OfficialFunctionIds.FUNC_GESTURE_LONG_PRESS
    val slideFuncId = OfficialFunctionIds.FUNC_GESTURE_SLIDE

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
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Single Press / Pinch Once (if supported or pinch device)
            if (showSinglePress) {
                val leftTitle = if (isPinch) "device_settings_left_press_once" else "device_settings_left_click"
                val rightTitle = if (isPinch) "device_settings_right_press_once" else "device_settings_right_click"
                XiaomiCardContainer {
                    XiaomiActionItem(
                        title = stringRes(leftTitle),
                        subtitle = formatGestureAction(gestureSettings.left.singleTap),
                        onClick = { editingTarget = GestureEditTarget(true, "single", singleFuncId, leftTitle) }
                    )
                    XiaomiItemDivider()
                    XiaomiActionItem(
                        title = stringRes(rightTitle),
                        subtitle = formatGestureAction(gestureSettings.right.singleTap),
                        onClick = { editingTarget = GestureEditTarget(false, "single", singleFuncId, rightTitle) }
                    )
                }
            }

            // 2. Double Tap / Press Twice
            if (showDoubleClick) {
                val leftTitle = if (isPinch) "device_settings_left_press_twice" else "device_settings_left_double_click"
                val rightTitle = if (isPinch) "device_settings_right_press_twice" else "device_settings_right_double_click"
                XiaomiCardContainer {
                    XiaomiActionItem(
                        title = stringRes(leftTitle),
                        subtitle = formatGestureAction(gestureSettings.left.doubleTap),
                        onClick = { editingTarget = GestureEditTarget(true, "double", doubleFuncId, leftTitle) }
                    )
                    XiaomiItemDivider()
                    XiaomiActionItem(
                        title = stringRes(rightTitle),
                        subtitle = formatGestureAction(gestureSettings.right.doubleTap),
                        onClick = { editingTarget = GestureEditTarget(false, "double", doubleFuncId, rightTitle) }
                    )
                }
            }

            // 3. Triple Tap / Press Triple
            if (showTripleClick) {
                val leftTitle = if (isPinch) "device_settings_left_press_triple" else "device_settings_left_triple_click"
                val rightTitle = if (isPinch) "device_settings_right_press_triple" else "device_settings_right_triple_click"
                XiaomiCardContainer {
                    XiaomiActionItem(
                        title = stringRes(leftTitle),
                        subtitle = formatGestureAction(gestureSettings.left.tripleTap),
                        onClick = { editingTarget = GestureEditTarget(true, "triple", tripleFuncId, leftTitle) }
                    )
                    XiaomiItemDivider()
                    XiaomiActionItem(
                        title = stringRes(rightTitle),
                        subtitle = formatGestureAction(gestureSettings.right.tripleTap),
                        onClick = { editingTarget = GestureEditTarget(false, "triple", tripleFuncId, rightTitle) }
                    )
                }
            }

            // 4. Long Press
            if (showLongPress) {
                XiaomiCardContainer {
                    XiaomiActionItem(
                        title = stringRes("device_settings_left_long_press"),
                        subtitle = formatGestureAction(gestureSettings.left.longPress),
                        onClick = { editingTarget = GestureEditTarget(true, "long", longFuncId, "device_settings_left_long_press") }
                    )
                    XiaomiItemDivider()
                    XiaomiActionItem(
                        title = stringRes("device_settings_right_long_press"),
                        subtitle = formatGestureAction(gestureSettings.right.longPress),
                        onClick = { editingTarget = GestureEditTarget(false, "long", longFuncId, "device_settings_right_long_press") }
                    )
                }
            }

            // 5. Slide
            if (showSlide) {
                XiaomiCardContainer {
                    XiaomiActionItem(
                        title = stringRes("device_settings_left_slide"),
                        subtitle = formatGestureAction(gestureSettings.left.slide),
                        onClick = { editingTarget = GestureEditTarget(true, "slide", slideFuncId, "device_settings_left_slide") }
                    )
                    XiaomiItemDivider()
                    XiaomiActionItem(
                        title = stringRes("device_settings_right_slide"),
                        subtitle = formatGestureAction(gestureSettings.right.slide),
                        onClick = { editingTarget = GestureEditTarget(false, "slide", slideFuncId, "device_settings_right_slide") }
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet to choose gesture action
    editingTarget?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { editingTarget = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringRes(target.titleKey),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val allowedActionIds = gestureCaps.allowedActions[target.funcId]
                val actions: List<GestureAction> = remember(target, allowedActionIds) {
                    if (!allowedActionIds.isNullOrEmpty()) {
                        allowedActionIds.map { actionId ->
                            GestureAction.fromId(actionId)
                        }
                    } else {
                        listOf(
                            GestureAction.PLAY_PAUSE,
                            GestureAction.NEXT_TRACK,
                            GestureAction.PREV_TRACK,
                            GestureAction.NOISE_CONTROL,
                            GestureAction.VOLUME_UP,
                            GestureAction.VOLUME_DOWN,
                            GestureAction.VOICE_ASSISTANT,
                            GestureAction.NONE
                        )
                    }
                }

                val earGestures = if (target.isLeft) gestureSettings.left else gestureSettings.right
                val currentAction = when (target.gestureType) {
                    "single" -> earGestures.singleTap
                    "double" -> earGestures.doubleTap
                    "triple" -> earGestures.tripleTap
                    "long" -> earGestures.longPress
                    else -> earGestures.slide
                }

                for (act in actions) {
                    val isCurrent = currentAction == act
                    val label = formatGestureAction(act)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                updateEarbudGesture(
                                    controller = controller,
                                    isLeft = target.isLeft,
                                    type = target.gestureType,
                                    newAction = act
                                )
                                editingTarget = null
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
    return stringRes(action.stringKey)
}
