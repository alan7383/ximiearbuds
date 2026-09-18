package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 authentic reproduction of `device_settings_fragment_xiao_ai_settings.xml`
 * (com.mi.earphone.settings.ui.xiaoai.XiaoAiSettingsFragment).
 */
@Composable
fun MiuixXiaoAiScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val xiaoAiState by controller.xiaoAiConfig.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XiaomiPageBg)
    ) {
        MiuixTopAppBar(
            title = stringRes("device_settings_super_aivs"),
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
            // Assistant Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(XiaomiCardBg)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(XiaomiCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = XiaomiCyan,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Text(
                        text = stringRes("device_settings_super_aivs_feature_guide_title"),
                        color = XiaomiTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringRes("device_settings_super_aivs_feature_guide_subtitle"),
                        color = XiaomiTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Group 1: Voice Triggers
            Text(
                text = stringRes("device_settings_super_aivs_wake_up_switch_title"),
                color = XiaomiTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )

            XiaomiCardContainer {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringRes("device_settings_super_aivs_wake_up_switch_title"),
                            color = XiaomiTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringRes("device_settings_awake_voice_assistant"),
                            color = XiaomiTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = xiaoAiState.wakeUpWordOpen,
                        onCheckedChange = { checked ->
                            controller.setXiaoAiConfig(xiaoAiState.copy(wakeUpWordOpen = checked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = XiaomiCyan
                        )
                    )
                }

                XiaomiItemDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringRes("device_settings_super_aivs_continuous_dialogue_duration_title"),
                            color = XiaomiTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringRes("device_settings_super_aivs_continuous_dialogue_duration_subtitle"),
                            color = XiaomiTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = xiaoAiState.continuousDialogueOpen,
                        onCheckedChange = { checked ->
                            controller.setXiaoAiConfig(xiaoAiState.copy(continuousDialogueOpen = checked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = XiaomiCyan
                        )
                    )
                }
            }

            // Group 2: Voice Timbre Selection
            Text(
                text = stringRes("device_settings_super_aivs_voice_tone_title"),
                color = XiaomiTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )

            XiaomiCardContainer {
                val timbres = listOf(
                    1 to (stringRes("super_aivs_tune_honey") to stringRes("device_settings_super_aivs_voice_tone_subtitle")),
                    2 to (stringRes("super_aivs_tune_galaxy") to stringRes("device_settings_super_aivs_voice_tone_subtitle"))
                )

                timbres.forEachIndexed { index, (timbreId, info) ->
                    val (title, subtitle) = info
                    val isSelected = xiaoAiState.timbre == timbreId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { controller.setXiaoAiConfig(xiaoAiState.copy(timbre = timbreId)) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = if (isSelected) XiaomiCyan else XiaomiTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                color = XiaomiTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = XiaomiCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (index < timbres.size - 1) {
                        XiaomiItemDivider()
                    }
                }
            }
        }
    }
}
