package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 authentic reproduction of `device_settings_fragment_sport_settings.xml`
 * (com.mi.earphone.settings.ui.sport.SportSettingsFragment).
 */
@Composable
fun MiuixSportSettingsScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swimState by controller.swimConfig.collectAsState()
    val scrollState = rememberScrollState()

    val meterFormat = stringRes("device_settings_meter")
    val formatMeter: (Int) -> String = { m ->
        if (meterFormat.contains("%1\$d") || meterFormat.contains("%d")) {
            meterFormat.replace("%1\$d", "$m").replace("%d", "$m")
        } else {
            "${m}m"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XiaomiPageBg)
    ) {
        MiuixTopAppBar(
            title = stringRes("device_settings_sport_config"),
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
            // Sport Header Card with official Xiaomi sport drawable
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
                    Image(
                        painter = painterResource("drawable/device_settings_sport_settings.png"),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = stringRes("device_settings_open_or_close_swim"),
                        color = XiaomiTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringRes("device_settings_download_health_hint"),
                        color = XiaomiTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Pool Length Selection (device_settings_sport_swim_length)
            Text(
                text = stringRes("device_settings_sport_swim_length"),
                color = XiaomiTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )

            XiaomiCardContainer {
                val lengths = listOf(
                    1 to (formatMeter(25) to 25),
                    2 to (formatMeter(50) to 50),
                    255 to (stringRes("device_settings_type_sound_personalized") to swimState.customLength)
                )

                lengths.forEachIndexed { index, (flag, info) ->
                    val (label, customLen) = info
                    val isSelected = swimState.currentLengthFlag == flag

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { controller.setSwimPoolLength(flag, customLen) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
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

                    if (index < lengths.size - 1) {
                        XiaomiItemDivider()
                    }
                }

                // Custom length slider when personalized (flag 255) is selected
                if (swimState.currentLengthFlag == 255) {
                    XiaomiItemDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringRes("device_settings_type_sound_personalized"),
                                fontSize = 13.sp,
                                color = XiaomiTextSecondary
                            )
                            Text(
                                text = formatMeter(swimState.customLength),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = XiaomiCyan
                            )
                        }
                        Slider(
                            value = swimState.customLength.toFloat(),
                            onValueChange = { controller.setSwimPoolLength(255, it.toInt()) },
                            valueRange = 15f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = XiaomiCyan,
                                activeTrackColor = XiaomiCyan
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = formatMeter(15), fontSize = 11.sp, color = XiaomiTextMuted)
                            Text(text = formatMeter(100), fontSize = 11.sp, color = XiaomiTextMuted)
                        }
                    }
                }
            }

            // Exercise Report Info Card (device_settings_exercise_report)
            Text(
                text = stringRes("device_settings_exercise_report"),
                color = XiaomiTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )

            XiaomiCardContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringRes("device_settings_download_health_hint"),
                        color = XiaomiTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
