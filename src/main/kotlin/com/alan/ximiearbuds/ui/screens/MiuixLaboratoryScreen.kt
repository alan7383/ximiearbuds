package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
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
import com.alan.ximiearbuds.ui.components.XiaomiActionItem
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 authentic reproduction of `device_settings_fragment_laboratory.xml`
 * (com.mi.earphone.settings.ui.lab.DeviceLaboratoryFragment).
 */
@Composable
fun MiuixLaboratoryScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    onNavigateToFitDetection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remindLostState by controller.remindLostConfig.collectAsState()
    val noiseControl by controller.noiseControl.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XiaomiPageBg)
    ) {
        MiuixTopAppBar(
            title = stringRes("device_settings_laboratory_function_title"),
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
            // Lab Hero Header
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
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = XiaomiCyan,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Text(
                        text = stringRes("device_settings_laboratory_function_title"),
                        color = XiaomiTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringRes("device_settings_function_settings"),
                        color = XiaomiTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Group 1: Anti-Lost Reminders
            Text(
                text = stringRes("device_settings_anti_disconnect_protection"),
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
                            text = stringRes("device_settings_anti_disconnect_protection"),
                            color = XiaomiTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringRes("device_settings_find_warn"),
                            color = XiaomiTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = remindLostState.isEnabled,
                        onCheckedChange = { checked ->
                            controller.setRemindLost(remindLostState.copy(isEnabled = checked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = XiaomiCyan
                        )
                    )
                }
            }

            // Group 2: Experimental Audio Calibration
            Text(
                text = stringRes("device_settings_sound_settings"),
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
                            text = stringRes("device_settings_personalized_noise_reduction"),
                            color = XiaomiTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringRes("device_settings_improved_noise_reduction_based_on_ear_canal_feature"),
                            color = XiaomiTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = noiseControl.isPersonalizedAnc,
                        onCheckedChange = { checked ->
                            controller.setPersonalizedNoiseReduction(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = XiaomiCyan
                        )
                    )
                }

                XiaomiItemDivider()

                XiaomiActionItem(
                    title = stringRes("device_settings_fit_detection"),
                    subtitle = stringRes("device_settings_perform_ear_canal_detection_before_personalized_noise_reduction"),
                    iconRes = "drawable/device_settings_laboratory_function.png",
                    onClick = onNavigateToFitDetection
                )
            }
        }
    }
}
