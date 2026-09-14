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
import com.alan.ximiearbuds.ui.components.XiaomiCardContainer
import com.alan.ximiearbuds.ui.components.XiaomiItemDivider

/**
 * 1:1 replica of Xiaomi Earbuds `device_settings_fragment_device_info.xml`.
 */
@Composable
fun MiuixDeviceInfoScreen(
    deviceInfo: TargetDeviceInfo,
    activeModel: EarbudsModel?,
    onBackClick: () -> Unit,
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
            title = "À propos de l'appareil",
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
                text = "INFORMATIONS MATÉRIELLES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                MiuixInfoRow("Modèle", activeModel?.commercialName ?: deviceInfo.name)
                XiaomiItemDivider()
                MiuixInfoRow("Code modèle", activeModel?.modelCode?.ifEmpty { activeModel.codename } ?: "M2110E1")
                XiaomiItemDivider()
                MiuixInfoRow("Identifiants USB/BT", "VID 0x${deviceInfo.vendorId.toString(16).uppercase()} / PID 0x${deviceInfo.productId.toString(16).uppercase()}")
                XiaomiItemDivider()
                MiuixInfoRow("Variante de couleur", "Type #${deviceInfo.colorType}")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section Header: Logiciel
            Text(
                text = "VERSION LOGICIELLE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            XiaomiCardContainer(modifier = Modifier.padding(horizontal = 12.dp)) {
                MiuixInfoRow("Version du firmware", deviceInfo.versionName.ifEmpty { "v1.0.4.8" })
                XiaomiItemDivider()
                MiuixInfoRow("Code version", "${deviceInfo.versionCode}")
                XiaomiItemDivider()
                MiuixInfoRow("Version protocole RCSP", "v2.1 (Actions/BES)")
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
