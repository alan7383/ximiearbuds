package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 Clone of official Xiaomi Empty State: device_settings_empty_layout.xml
 */
@Composable
fun XiaomiEmptyStateView(
    onAddDeviceClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            // Official Empty State Illustration (device_list_empty.webp)
            Image(
                painter = painterResource("drawable/device_list_empty.webp"),
                contentDescription = null,
                modifier = Modifier
                    .size(160.dp)
                    .padding(bottom = 20.dp)
            )

            // Title: @string/device_no_available_device
            Text(
                text = stringRes("device_no_available_device"),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = XiaomiTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle: @string/device_no_paired_device_tip
            Text(
                text = stringRes("device_no_paired_device_tip"),
                fontSize = 13.5.sp,
                color = XiaomiTextMuted,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Positive Action Button: @string/device_add_title ("Ajouter des écouteurs")
            Button(
                onClick = onAddDeviceClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = XiaomiCyan,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringRes("device_add_title"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
