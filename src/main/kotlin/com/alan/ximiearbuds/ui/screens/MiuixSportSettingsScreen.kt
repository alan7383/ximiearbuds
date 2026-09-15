package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pool
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
            // Sport Header Card
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
                            imageVector = Icons.Default.Pool,
                            contentDescription = null,
                            tint = XiaomiCyan,
                            modifier = Modifier.size(34.dp)
                        )
                    }
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

            // Pool Length Selection
            Text(
                text = stringRes("device_settings_open_or_close_swim"),
                color = XiaomiTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )

            XiaomiCardContainer {
                val lengths = listOf(
                    1 to ("25 m" to 25),
                    2 to ("50 m" to 50),
                    255 to ("33 m" to 33)
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
            }

            // Exercise Report Info Card
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Dernière séance",
                            color = XiaomiTextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Aujourd'hui, 45 min",
                            color = XiaomiTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    XiaomiItemDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Distance totale",
                            color = XiaomiTextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "1 250 m (50 longueurs)",
                            color = XiaomiCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
