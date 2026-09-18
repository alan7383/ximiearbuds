package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
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
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiDeviceImage
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 authentic reproduction of `device_settings_fragment_personal_skin.xml`
 * (com.mi.earphone.settings.ui.skin.PersonalSkinFragment).
 */
@Composable
fun MiuixPersonalSkinScreen(
    controller: EarbudsController,
    activeModel: EarbudsModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceInfo by controller.deviceInfo.collectAsState()
    val scrollState = rememberScrollState()

    // Color definitions mapping color IDs to visual tints and names
    val availableColors = listOf(
        1 to (stringRes("device_settings_skin_white") to Color(0xFFF0F0F2)),
        2 to (stringRes("device_settings_skin_black") to Color(0xFF222226)),
        3 to (stringRes("device_settings_skin_silver") to Color(0xFFB0B3B8)),
        4 to (stringRes("device_settings_skin_blue") to Color(0xFF4A90E2)),
        7 to (stringRes("device_settings_skin_gold") to Color(0xFFE5A93C)),
        9 to (stringRes("device_settings_skin_titanium") to Color(0xFF8E8E93)),
        10 to (stringRes("device_settings_skin_green") to Color(0xFF2E7D32))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XiaomiPageBg)
    ) {
        MiuixTopAppBar(
            title = stringRes("device_settings_skin_title"),
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Preview Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(XiaomiCardBg),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    XiaomiDeviceImage(
                        model = activeModel,
                        colorType = deviceInfo.colorType,
                        modifier = Modifier.size(110.dp)
                    )
                    Text(
                        text = activeModel.commercialName,
                        color = XiaomiTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Skin / Colorway Selector
            Text(
                text = stringRes("device_settings_skin_choose"),
                color = XiaomiTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )

            XiaomiCardContainer {
                availableColors.forEachIndexed { index, (colorId, info) ->
                    val (colorName, colorHex) = info
                    val isSelected = deviceInfo.colorType == colorId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { controller.setDeviceColor(colorId) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(colorHex)
                                    .border(1.dp, Color(0x33888888), CircleShape)
                            )
                            Text(
                                text = colorName,
                                color = if (isSelected) XiaomiCyan else XiaomiTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }

                        if (isSelected) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringRes("device_settings_skin_using"),
                                    color = XiaomiCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = XiaomiCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (index < availableColors.size - 1) {
                        XiaomiItemDivider()
                    }
                }
            }
        }
    }
}
