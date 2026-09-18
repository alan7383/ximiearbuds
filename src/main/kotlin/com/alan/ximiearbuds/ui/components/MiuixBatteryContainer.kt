package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.protocol.BatteryInfo
import com.alan.ximiearbuds.core.protocol.TargetDeviceInfo
import com.alan.ximiearbuds.ui.theme.stringRes

/**
 * Authentic 1:1 replica of Xiaomi Earbuds `device_settings_layout_battery.xml` and `BatteryView.java`.
 * 
 * Features:
 * - 3 equal columns for Left Earbud, Right Earbud, and Case
 * - Official `device_settings_battery_frame.webp` and `device_settings_battery_dot.webp`
 * - Dynamic color bar (Green #00C853 > 20%, Red #FF3B30 <= 20%)
 * - Official `device_settings_charging.webp` lightning indicator when plugged in
 * - Exact vertical hairline dividers (#33206fe5) and MIUI text styling
 */
@Composable
fun MiuixBatteryContainer(
    deviceInfo: TargetDeviceInfo,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val containerBg = if (isDark) Color(0xFF191919) else Color(0xFFF2F3F5)
    val dividerColor = Color(0x33206FE5)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .padding(vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Earbud Column
            MiuixBatteryColumn(
                label = stringRes("device_settings_left"),
                percentage = if (deviceInfo.leftBattery.isConnected && deviceInfo.leftBattery.percentage >= 0) deviceInfo.leftBattery.percentage else null,
                isCharging = deviceInfo.leftBattery.isCharging,
                modifier = Modifier.weight(1f)
            )

            // Divider 1
            Box(
                modifier = Modifier
                    .width(0.7.dp)
                    .height(44.dp)
                    .background(dividerColor)
            )

            // Right Earbud Column
            MiuixBatteryColumn(
                label = stringRes("device_settings_right"),
                percentage = if (deviceInfo.rightBattery.isConnected && deviceInfo.rightBattery.percentage >= 0) deviceInfo.rightBattery.percentage else null,
                isCharging = deviceInfo.rightBattery.isCharging,
                modifier = Modifier.weight(1f)
            )

            // Divider 2
            Box(
                modifier = Modifier
                    .width(0.7.dp)
                    .height(44.dp)
                    .background(dividerColor)
            )

            // Charging Case Column
            MiuixBatteryColumn(
                label = stringRes("device_settings_box"),
                percentage = if (deviceInfo.caseBattery.isConnected && deviceInfo.caseBattery.percentage >= 0) deviceInfo.caseBattery.percentage else null,
                isCharging = deviceInfo.caseBattery.isCharging,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MiuixBatteryColumn(
    label: String,
    percentage: Int?,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Official BatteryView
        MiuixBatteryGauge(
            percentage = percentage ?: 0,
            isCharging = isCharging
        )

        Spacer(modifier = Modifier.height(5.dp))

        // Percentage Text (DeviceSettingsCharge : 15sp FontRegular text_color)
        Text(
            text = percentage?.let { "$it%" } ?: "--",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Subtitle Label (DeviceSettingsBattery : 12sp text_color_50 = 40%)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

/**
 * Pixel-accurate Compose implementation of `com.mi.earphone.settings.ui.battery.BatteryView`.
 */
@Composable
fun MiuixBatteryGauge(
    percentage: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val fillColor = if (percentage > 20) Color(0xFF00C853) else Color(0xFFFF3B30)
    val frameColorFilter = if (isDark) ColorFilter.tint(Color.White.copy(alpha = 0.85f)) else null

    Box(
        modifier = modifier.size(width = 32.dp, height = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Battery Body Frame & Dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(15.dp)
            ) {
                // Internal dynamic progress fill
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val paddingH = 2.2.dp.toPx()
                    val paddingV = 2.2.dp.toPx()
                    val fullFillWidth = size.width - (paddingH * 2)
                    val fillHeight = size.height - (paddingV * 2)
                    val progressWidth = (percentage.coerceIn(0, 100) / 100f) * fullFillWidth

                    if (progressWidth > 0f) {
                        drawRoundRect(
                            color = fillColor,
                            topLeft = Offset(paddingH, paddingV),
                            size = Size(progressWidth, fillHeight),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }

                // Battery Frame Outline
                Image(
                    painter = painterResource("drawable/device_settings_battery_frame.webp"),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = frameColorFilter
                )

                // Charging Lightning Bolt Overlay
                if (isCharging) {
                    Image(
                        painter = painterResource("drawable/device_settings_charging.webp"),
                        contentDescription = "En charge",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(11.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(1.dp))

            // Battery Anode Dot
            Image(
                painter = painterResource("drawable/device_settings_battery_dot.webp"),
                contentDescription = null,
                modifier = Modifier.size(width = 2.dp, height = 6.dp),
                colorFilter = frameColorFilter
            )
        }
    }
}
