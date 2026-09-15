package com.alan.ximiearbuds.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.SpatialAudio
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
import com.alan.ximiearbuds.core.protocol.OfficialPayloadCodecs
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 authentic reproduction of `device_settings_activity_spatial_audio.xml`
 * (com.mi.earphone.settings.ui.spatialaudio.PersonalAudioFragment).
 */
@Composable
fun MiuixSpatialAudioScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spatialState by controller.spatialAudioConfig.collectAsState()
    val scrollState = rememberScrollState()

    // Smooth head orientation simulation pulse
    val infiniteTransition = rememberInfiniteTransition(label = "spatialPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XiaomiPageBg)
    ) {
        MiuixTopAppBar(
            title = stringRes("device_settings_spatial_audio_audio_mode"),
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 3D Visualizer Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(XiaomiCardBg),
                contentAlignment = Alignment.Center
            ) {
                // Radial spatial concentric rings
                Box(
                    modifier = Modifier
                        .size((140 * (if (spatialState.isOpen) pulseScale else 1f)).dp)
                        .clip(CircleShape)
                        .background(if (spatialState.isOpen) XiaomiCyan.copy(alpha = 0.12f) else Color(0x0A888888))
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(if (spatialState.isOpen) XiaomiCyan.copy(alpha = 0.22f) else Color(0x14888888))
                )
                Icon(
                    imageVector = Icons.Default.SpatialAudio,
                    contentDescription = null,
                    tint = if (spatialState.isOpen) XiaomiCyan else XiaomiTextSecondary,
                    modifier = Modifier.size(54.dp)
                )
            }

            // Group 1: Core Spatial Audio Toggles
            XiaomiCardContainer {
                // Main Switch: Spatial Audio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringRes("device_settings_spatial_audio_title_m75a"),
                            color = XiaomiTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringRes("device_settings_spatial_audio_effect"),
                            color = XiaomiTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Switch(
                        checked = spatialState.isOpen,
                        onCheckedChange = { checked ->
                            controller.setSpatialAudioFull(spatialState.copy(isOpen = checked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = XiaomiCyan
                        )
                    )
                }

                if (spatialState.isOpen) {
                    XiaomiItemDivider()

                    // Head Tracking Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringRes("device_settings_spatial_audio_head_tracking"),
                                color = XiaomiTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringRes("device_settings_spatial_audio_head_tracking_desc"),
                                color = XiaomiTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Switch(
                            checked = spatialState.headTracking,
                            onCheckedChange = { checked ->
                                controller.setSpatialAudioFull(spatialState.copy(headTracking = checked))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = XiaomiCyan
                            )
                        )
                    }

                    XiaomiItemDivider()

                    // Notification Sound Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringRes("device_settings_spatial_audio_notify_sound"),
                                color = XiaomiTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringRes("device_settings_spatial_audio_notify_sound_title"),
                                color = XiaomiTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = spatialState.virtualSurround,
                            onCheckedChange = { checked ->
                                controller.setSpatialAudioFull(spatialState.copy(virtualSurround = checked))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = XiaomiCyan
                            )
                        )
                    }
                }
            }

            // Group 2: Scene Rendering Modes (if Spatial Audio is open)
            if (spatialState.isOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringRes("device_settings_scene_rendering"),
                        color = XiaomiTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp)
                    )

                    XiaomiCardContainer {
                        val scenes = listOf(
                            0 to stringRes("device_settings_spatial_audio_default"),
                            1 to stringRes("device_settings_spatial_audio_theater"),
                            2 to stringRes("device_settings_spatial_audio_game"),
                            3 to stringRes("device_settings_spatial_audio_music")
                        )

                        scenes.forEachIndexed { index, (modeId, modeTitle) ->
                            val isSelected = spatialState.sceneRenderingMode == modeId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        controller.setSpatialAudioFull(spatialState.copy(sceneRenderingMode = modeId))
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = modeTitle.ifBlank { "Mode $modeId" },
                                    color = if (isSelected) XiaomiCyan else XiaomiTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = XiaomiCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (index < scenes.size - 1) {
                                XiaomiItemDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
