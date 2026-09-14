package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.protocol.TargetDeviceInfo
import com.alan.ximiearbuds.ui.theme.*

@Composable
fun XiaomiBatteryCapsule(
    deviceInfo: TargetDeviceInfo,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(XiaomiCardBg)
            .border(1.dp, XiaomiCardBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Earbud (Index 0)
            BatteryItem(
                label = stringRes("device_settings_left"),
                level = deviceInfo.leftBattery.percentage,
                isCharging = deviceInfo.leftBattery.isCharging,
                isRight = false,
                modifier = Modifier.weight(1f)
            )

            // Divider 1 (#33206fe5 from official XML)
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(38.dp)
                    .background(XiaomiBatteryDivider)
            )

            // Right Earbud (Index 1)
            BatteryItem(
                label = stringRes("device_settings_right"),
                level = deviceInfo.rightBattery.percentage,
                isCharging = deviceInfo.rightBattery.isCharging,
                isRight = true,
                modifier = Modifier.weight(1f)
            )

            // Divider 2
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(38.dp)
                    .background(XiaomiBatteryDivider)
            )

            // Case (Index 2)
            CaseBatteryItem(
                label = stringRes("device_settings_box"),
                level = deviceInfo.caseBattery.percentage,
                isCharging = deviceInfo.caseBattery.isCharging,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BatteryItem(
    label: String,
    level: Int,
    isCharging: Boolean,
    isRight: Boolean,
    modifier: Modifier = Modifier
) {
    val displayLevel = if (level >= 0) "$level%" else "--"

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon + Battery percent row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource("drawable/device_settings_ic_earphone.png"),
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .scale(scaleX = if (isRight) -1f else 1f, scaleY = 1f)
            )

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = displayLevel,
                color = XiaomiTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            if (isCharging) {
                Spacer(modifier = Modifier.width(3.dp))
                Image(
                    painter = painterResource("drawable/device_settings_charging.webp"),
                    contentDescription = "Charging",
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Label ("G" / "D" or "L" / "R")
        Text(
            text = label,
            color = XiaomiTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun CaseBatteryItem(
    label: String,
    level: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val displayLevel = if (level >= 0) "$level%" else "--"

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Case Battery indicator row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Case outline representation
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(11.dp)
                    .border(1.2.dp, XiaomiTextSecondary, RoundedCornerShape(3.dp))
                    .padding(1.5.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (level >= 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(level.coerceIn(0, 100) / 100f)
                            .background(
                                if (level > 20) XiaomiGreen else XiaomiOrange,
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = displayLevel,
                color = XiaomiTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            if (isCharging) {
                Spacer(modifier = Modifier.width(3.dp))
                Image(
                    painter = painterResource("drawable/device_settings_charging.webp"),
                    contentDescription = "Charging",
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Label ("Boîtier" / "Case")
        Text(
            text = label,
            color = XiaomiTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
