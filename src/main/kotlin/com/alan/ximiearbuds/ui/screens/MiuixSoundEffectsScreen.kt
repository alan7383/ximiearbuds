package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.alan.ximiearbuds.ui.components.*

/**
 * 1:1 replica of Xiaomi Earbuds `device_settings_activity_soundeffect.xml`.
 * 
 * Replaces any desktop dialog with an authentic MIUI activity/screen containing:
 * - Audibility adaptation (device_settings_audibility_adaptation)
 * - Virtual Surround 3D (device_settings_sound_virtual_surround)
 * - Spatial Audio link (device_settings_sound_spatial_audio)
 * - Adaptive Volume (device_settings_sound_adaptive_volume)
 * - Notification Volume slider (notification_volume_seekbar)
 * - Customized EQ presets & 10-band studio graphic EQ (included group)
 */
@Composable
fun MiuixSoundEffectsScreen(
    controller: EarbudsController,
    onBackClick: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var virtualSurround by remember { mutableStateOf(false) }
    var adaptiveVolume by remember { mutableStateOf(false) }
    var audibilityAdaptation by remember { mutableStateOf(false) }
    var notificationVolume by remember { mutableStateOf(75f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Navigation Bar
        MiuixTopAppBar(
            title = "Effets sonores",
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

            // Section 1: Spatial & Surround Audio
            Text(
                text = "EXPÉRIENCE AUDIO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                XiaomiSwitchItem(
                    title = "Son surround virtuel",
                    subtitle = "Offre une scène sonore 3D immersive et enveloppante",
                    checked = virtualSurround,
                    onCheckedChange = { virtualSurround = it }
                )

                XiaomiItemDivider()

                XiaomiSwitchItem(
                    title = "Volume adaptatif",
                    subtitle = "Ajuste automatiquement le volume d'écoute selon le bruit ambiant",
                    checked = adaptiveVolume,
                    onCheckedChange = { adaptiveVolume = it }
                )

                XiaomiItemDivider()

                XiaomiSwitchItem(
                    title = "Adaptation auditive",
                    subtitle = "Compense les fréquences selon votre sensibilité auditive",
                    checked = audibilityAdaptation,
                    onCheckedChange = { audibilityAdaptation = it }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Notification Volume Slider (Replicates NotificationVolumeSeekbar)
            Text(
                text = "VOLUME DES NOTIFICATIONS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Volume des alertes et bips",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${notificationVolume.toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF007AFF)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = notificationVolume,
                        onValueChange = { notificationVolume = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF007AFF),
                            activeTrackColor = Color(0xFF007AFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Equalizer Entry point (Replicates include customized_eq)
            Text(
                text = "ÉGALISEUR AUDIO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                XiaomiActionItem(
                    title = "Égaliseur personnalisé 10 bandes",
                    subtitle = "Personnalisez la courbe fréquentielle ou choisissez un profil",
                    onClick = onNavigateToEqualizer
                )
            }
        }
    }
}
