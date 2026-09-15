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
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiSwitchItem
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.theme.XiaomiCyan
import com.alan.ximiearbuds.ui.theme.stringRes

/**
 * 1:1 replica of Xiaomi Earbuds `device_settings_fragment_earbox_sound.xml`.
 * Fully wired to official MIUI strings.
 */
@Composable
fun MiuixEarboxSoundScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var openSound by remember { mutableStateOf(true) }
    var closeSound by remember { mutableStateOf(true) }
    var chargeSound by remember { mutableStateOf(true) }
    var volume by remember { mutableStateOf(75f) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Navigation Bar
        MiuixTopAppBar(
            title = stringRes("device_settings_earbox_sound"),
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

            // Volume Section Header
            Text(
                text = stringRes("notification_volume").ifBlank { "Volume" }.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            // Volume Card
            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringRes("device_settings_earbox_sound"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${volume.toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = XiaomiCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = XiaomiCyan,
                            activeTrackColor = XiaomiCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Events Section Header
            Text(
                text = stringRes("device_settings_function_settings").uppercase(),
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
                    title = stringRes("device_settings_earbox_opening_sound"),
                    subtitle = stringRes("device_settings_earbox_opening_sound"),
                    checked = openSound,
                    onCheckedChange = { openSound = it }
                )

                XiaomiItemDivider()

                XiaomiSwitchItem(
                    title = stringRes("device_settings_earbox_closing_sound"),
                    subtitle = stringRes("device_settings_earbox_closing_sound"),
                    checked = closeSound,
                    onCheckedChange = { closeSound = it }
                )

                XiaomiItemDivider()

                XiaomiSwitchItem(
                    title = stringRes("device_settings_earbox_charge_sound"),
                    subtitle = stringRes("device_settings_earbox_charge_sound"),
                    checked = chargeSound,
                    onCheckedChange = { chargeSound = it }
                )
            }
        }
    }
}
