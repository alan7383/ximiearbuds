package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 authentic reproduction of official Xiaomi Empty State:
 * device_settings_empty_layout.xml
 *
 * Layout hierarchy:
 * - ConstraintLayout (match_parent)
 *   - TextView @id/ic_tv (drawableTop = device_list_empty, text = @string/device_no_available_device, 14sp, text_color_40)
 *   - TextView @id/add (text = @string/device_no_paired_device_tip, 14sp, text_color_40, margin 20dp)
 *   - TextView @id/add_view (BaseButton.Positive style, 16sp white, radius 180dp, margins 27dp, text = @string/device_add_title)
 */
@Composable
fun XiaomiEmptyStateView(
    onAddDeviceClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    val textMutedColor = if (isDarkTheme) XiaomiTextMuted else XiaomiLightTextMuted
    val emptyIllustration = if (isDarkTheme) {
        "drawable/device_list_empty_night.webp"
    } else {
        "drawable/device_list_empty.webp"
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Centered section: ic_tv + add
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp)
                .widthIn(max = 480.dp)
        ) {
            // @id/ic_tv: drawableTop = device_list_empty
            Image(
                painter = painterResource(emptyIllustration),
                contentDescription = null,
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(10.dp)) // android:drawablePadding="10dp"

            // @id/ic_tv text: @string/device_no_available_device
            Text(
                text = stringRes("device_no_available_device"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = textMutedColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // @id/add text: @string/device_no_paired_device_tip
            Text(
                text = stringRes("device_no_paired_device_tip"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = textMutedColor,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // Bottom-pinned button: @id/add_view (style=@style/BaseButton.Positive)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 27.dp, end = 27.dp, bottom = 27.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onAddDeviceClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(180.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = XiaomiElectricBlue,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = stringRes("device_add_title"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}
