package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.ui.theme.*

import com.alan.ximiearbuds.core.device.EarbudsModel

@Composable
fun XiaomiHeroBanner(
    model: EarbudsModel? = null,
    deviceName: String,
    isConnected: Boolean,
    colorType: Int = 0,
    onColorSelected: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val effectiveColor = if (colorType > 0) colorType else model?.defaultColor ?: 1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Device Image with dynamic color resolution
        Box(
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            XiaomiDeviceImage(
                model = model,
                colorType = effectiveColor,
                contentDescription = deviceName,
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Device Title
        Text(
            text = deviceName,
            color = XiaomiTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Connection Status Badge (MIUI style)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) XiaomiGreen else XiaomiRed)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = if (isConnected) stringRes("device_connected") else stringRes("device_disconnected"),
                color = if (isConnected) XiaomiBlue else XiaomiTextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // Color Swatches (if multiple official color variants exist for this model)
        if (model != null && model.colorVariants.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sortedVariants = model.colorVariants.keys.mapNotNull { it.toIntOrNull() }.sorted()
                for (cid in sortedVariants) {
                    val isSelected = cid == effectiveColor
                    val swatchColor = when (cid) {
                        1 -> Color(0xFFEAEAEA) // White
                        2 -> Color(0xFF2C2D31) // Black / Dark Grey
                        3 -> Color(0xFF6E9E88) // Cyan / Forest Green
                        4 -> Color(0xFF5B789B) // Blue
                        5 -> Color(0xFFC5C7DF) // Lavender / Purple
                        6 -> Color(0xFFE2B5BE) // Pink
                        7 -> Color(0xFFD6B570) // Gold
                        9 -> Color(0xFF867297) // Deep Purple
                        10 -> Color(0xFF42566F) // Navy Blue
                        else -> Color(0xFF7A7E85)
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) XiaomiCyan.copy(alpha = 0.35f) else Color.Transparent
                            )
                            .clickable {
                                onColorSelected?.invoke(cid)
                            }
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 18.dp else 16.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                        )
                    }
                }
            }
        }
    }
}
