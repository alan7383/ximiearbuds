package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiActionItem
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.components.XiaomiSwitchItem

/**
 * 1:1 replica of Xiaomi Earbuds `device_settings_fragment_set_more.xml`.
 */
@Composable
fun MiuixMoreSettingsScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    onNavigateToEarbox: () -> Unit,
    onNavigateToFitDetection: () -> Unit,
    onNavigateToDeviceInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quickSettings by controller.quickSettings.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Authentic Top Navigation Bar
        MiuixTopAppBar(
            title = "Plus de paramètres",
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

            // Section Header: Paramètres des fonctionnalités
            Text(
                text = "PARAMÈTRES DES FONCTIONNALITÉS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                // In-Ear Wear Detection
                XiaomiSwitchItem(
                    title = "Détection intra-auriculaire",
                    subtitle = "Met en pause la musique quand un écouteur est retiré",
                    checked = quickSettings.inEarDetection,
                    onCheckedChange = { controller.setInEarDetection(it) }
                )

                XiaomiItemDivider()

                // Dual Device Multipoint
                XiaomiSwitchItem(
                    title = "Connexion double appareil",
                    subtitle = "Basculez automatiquement entre votre PC et votre smartphone",
                    checked = quickSettings.multipoint,
                    onCheckedChange = { controller.setMultipoint(it) }
                )

                XiaomiItemDivider()

                // Low Latency Gaming Mode
                XiaomiSwitchItem(
                    title = "Mode jeu faible latence",
                    subtitle = "Réduit la latence Bluetooth pour le gaming et la vidéo",
                    checked = quickSettings.lowLatency,
                    onCheckedChange = { controller.setLowLatency(it) }
                )

                XiaomiItemDivider()

                // Fit Detection
                XiaomiActionItem(
                    title = "Test d'ajustement des embouts",
                    subtitle = "Vérifie l'étanchéité acoustique des embouts",
                    onClick = onNavigateToFitDetection
                )

                XiaomiItemDivider()

                // Earbox Sound
                XiaomiActionItem(
                    title = "Sons et alertes du boîtier",
                    subtitle = "Personnaliser les alertes d'ouverture et de charge",
                    onClick = onNavigateToEarbox
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section Header: Autre
            Text(
                text = "AUTRE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                // Device Info
                XiaomiActionItem(
                    title = "À propos de l'appareil",
                    subtitle = "Version logicielle, adresse MAC et statut matériel",
                    onClick = onNavigateToDeviceInfo
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Unpair Device Destructive Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        controller.disconnect()
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x1AFF3B30),
                        contentColor = Color(0xFFFF3B30)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Dissocier cet appareil",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
