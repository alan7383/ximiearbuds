package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Usb
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
 * 1:1 authentic reproduction of `device_settings_fragment_usb.xml`
 * (com.mi.earphone.settings.ui.usb.DongleSettingsFragment).
 */
@Composable
fun MiuixDongleSettingsScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dongleState by controller.dongleConfig.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XiaomiPageBg)
    ) {
        MiuixTopAppBar(
            title = stringRes("device_settings_usb_update"),
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
            // Dongle Status Hero
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
                            imageVector = Icons.Default.Usb,
                            contentDescription = null,
                            tint = XiaomiCyan,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Text(
                        text = "Émetteur sans fil USB 2.4 GHz",
                        color = XiaomiTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Connecté • Mode ultra-basse latence actif",
                        color = XiaomiGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Group 1: Transmission Modes
            Text(
                text = "Mode de transmission",
                color = XiaomiTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )

            XiaomiCardContainer {
                val modes = listOf(
                    0 to ("Audio haute fidélité sans perte" to "Bande passante audio maximale pour la musique"),
                    1 to ("Mode Gaming ultra-basse latence" to "Latence minimale pour les jeux compétitifs (20 ms)"),
                    2 to ("Mode micro haute définition" to "Privilégie la clarté du microphone en direct")
                )

                modes.forEachIndexed { index, (modeId, info) ->
                    val (title, subtitle) = info
                    val isSelected = dongleState.dongleMode == modeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { controller.setDongleMode(modeId) }
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
                    if (index < modes.size - 1) {
                        XiaomiItemDivider()
                    }
                }
            }

            // Group 2: Monitoring Volume
            Text(
                text = "Retour moniteur casque",
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
                            text = "Retour microphone en temps réel",
                            color = XiaomiTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Entendre votre propre voix dans les écouteurs",
                            color = XiaomiTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = dongleState.monitorSwitch,
                        onCheckedChange = { checked ->
                            controller.setDongleMonitor(checked, dongleState.monitorVolume)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = XiaomiCyan
                        )
                    )
                }

                if (dongleState.monitorSwitch) {
                    XiaomiItemDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Volume du retour",
                                color = XiaomiTextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${dongleState.monitorVolume}%",
                                color = XiaomiCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Slider(
                            value = dongleState.monitorVolume.toFloat(),
                            onValueChange = { vol ->
                                controller.setDongleMonitor(true, vol.toInt())
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = XiaomiCyan,
                                activeTrackColor = XiaomiCyan,
                                inactiveTrackColor = XiaomiCardHover
                            )
                        )
                    }
                }
            }
        }
    }
}
