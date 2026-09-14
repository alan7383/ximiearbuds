package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alan.ximiearbuds.core.device.EarbudsModel
import com.alan.ximiearbuds.core.protocol.TargetDeviceInfo
import com.alan.ximiearbuds.ui.theme.*

@Composable
fun XiaomiDeviceAboutDialog(
    deviceInfo: TargetDeviceInfo,
    activeModel: EarbudsModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(XiaomiCardBg)
                .border(1.dp, XiaomiCardBorder, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringRes("device_settings_about_device"),
                        color = XiaomiTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = XiaomiTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF141416))
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        InfoRow(stringRes("device_settings_device_model"), activeModel.commercialName)
                        InfoDivider()
                        InfoRow(stringRes("device_settings_device_name"), deviceInfo.name.ifBlank { activeModel.commercialName })
                        InfoDivider()
                        InfoRow(stringRes("device_settings_device_firmware_version"), deviceInfo.versionName.ifBlank { "1.0.8.2" })
                        InfoDivider()
                        InfoRow("Vendor ID (VID)", "0x%04X (%d)".format(deviceInfo.vendorId, deviceInfo.vendorId))
                        InfoDivider()
                        InfoRow("Product ID (PID)", "0x%04X (%d)".format(deviceInfo.productId, deviceInfo.productId))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = XiaomiTextSecondary,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = XiaomiTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(XiaomiDivider)
    )
}
