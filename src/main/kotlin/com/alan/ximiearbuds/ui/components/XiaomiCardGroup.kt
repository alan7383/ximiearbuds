package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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

@Composable
fun XiaomiCardContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(XiaomiCardBg)
            .border(1.dp, XiaomiCardBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
fun XiaomiActionItem(
    title: String,
    subtitle: String? = null,
    iconRes: String? = null,
    badgeText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = XiaomiTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = XiaomiTextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 16.sp
                )
            }
        }

        if (!badgeText.isNullOrBlank()) {
            Text(
                text = badgeText,
                color = XiaomiTextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Image(
            painter = painterResource("drawable/right_arrow_icon.webp"),
            contentDescription = null,
            modifier = Modifier.size(width = 8.dp, height = 13.dp)
        )
    }
}

@Composable
fun XiaomiSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    iconRes: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = XiaomiTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = XiaomiTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 15.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = XiaomiCyan,
                uncheckedThumbColor = Color(0xFFAAAAAA),
                uncheckedTrackColor = Color(0xFF333336),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun XiaomiItemDivider(
    hasIcon: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = if (hasIcon) 54.dp else 16.dp, end = 16.dp)
            .height(1.dp)
            .background(XiaomiDivider)
    )
}
