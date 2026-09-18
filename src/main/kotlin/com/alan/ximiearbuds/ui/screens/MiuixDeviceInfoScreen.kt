package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.core.protocol.TargetDeviceInfo
import com.alan.ximiearbuds.ui.components.MiuixTopAppBar
import com.alan.ximiearbuds.ui.components.XiaomiActionItem
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider
import com.alan.ximiearbuds.ui.theme.stringRes

/**
 * 1:1 replica of Xiaomi Earbuds `device_settings_fragment_device_info.xml`.
 */
@Composable
fun MiuixDeviceInfoScreen(
    deviceInfo: TargetDeviceInfo,
    activeModel: EarbudsModel?,
    onBackClick: () -> Unit,
    onNavigateToGuide: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Navigation Bar
        MiuixTopAppBar(
            title = stringRes("device_settings_about_device"),
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

            // Section Header: Matériel
            Text(
                text = stringRes("device_settings_about_device").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                MiuixInfoRow(stringRes("device_settings_device_model"), activeModel?.commercialName ?: deviceInfo.name)
                XiaomiItemDivider()
                MiuixInfoRow(stringRes("device_settings_device_mac"), deviceInfo.address.ifEmpty { "11:22:33:44:55:66" })
                XiaomiItemDivider()
                MiuixInfoRow(stringRes("device_settings_device_serial_num"), deviceInfo.serialNumber.ifEmpty { "SN892170349182" })
                XiaomiItemDivider()
                MiuixInfoRow(stringRes("device_settings_skin_title"), "Type #${deviceInfo.colorType}")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section Header: Logiciel
            Text(
                text = stringRes("device_settings_check_update").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                MiuixInfoRow(stringRes("device_settings_device_firmware_version"), deviceInfo.versionName.ifEmpty { "v1.0.4.8" })
                XiaomiItemDivider()
                MiuixInfoRow(stringRes("device_settings_device_fw_version"), "${deviceInfo.versionCode}")
            }

            if (onNavigateToGuide != null) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringRes("device_settings_beginner_guide").uppercase(),
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
                        title = stringRes("device_settings_beginner_guide"),
                        subtitle = stringRes("device_settings_record_settings_guide_subtitle"),
                        iconRes = "drawable/device_settings_ic_about_device.webp",
                        onClick = onNavigateToGuide
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
