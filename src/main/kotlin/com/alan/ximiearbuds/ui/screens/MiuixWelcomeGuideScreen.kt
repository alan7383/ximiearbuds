package com.alan.ximiearbuds.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.ximiearbuds.core.device.DevicePreferences
import com.alan.ximiearbuds.ui.theme.*

/**
 * 1:1 Authentic reproduction of official Xiaomi Earbuds Welcome Guide:
 * - Layout: `login_activity_guide.xml`
 * - Items: `login_layout_guide_item.xml`
 * - Controller/Activity: `com.xiaomi.fitness.login.guide.GuideActivity`
 * 
 * 4-slide onboarding presentation with official MIUI illustrations:
 * 1. Firmware update (login_guide_one.png)
 * 2. Noise cancellation (login_guide_two.png)
 * 3. Find earphones (login_guide_three.png)
 * 4. In-ear detection (login_guide_four.png)
 */
@Composable
fun MiuixWelcomeGuideScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(0) }

    data class GuidePage(
        val titleRes: String,
        val subtitleRes: String,
        val imageRes: String
    )

    val pages = remember {
        listOf(
            GuidePage(
                titleRes = "login_guide_fw_update",
                subtitleRes = "login_guide_fw_update_detail",
                imageRes = "drawable/login_guide_one.png"
            ),
            GuidePage(
                titleRes = "login_guide_anc",
                subtitleRes = "login_guide_anc_detail",
                imageRes = "drawable/login_guide_two.png"
            ),
            GuidePage(
                titleRes = "login_guide_find_earphone",
                subtitleRes = "login_guide_find_earphone_detail",
                imageRes = "drawable/login_guide_three.png"
            ),
            GuidePage(
                titleRes = "login_guide_fit_detect",
                subtitleRes = "login_guide_fit_detect_detail",
                imageRes = "drawable/login_guide_four.png"
            )
        )
    }

    val isLastPage = currentPage == pages.size - 1

    fun finishGuide() {
        DevicePreferences.setWelcomeFinished(true)
        onFinish()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Slide Content
        val page = pages[currentPage]

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title (login_layout_guide_item: @id/title - 28sp Medium)
            Text(
                text = stringRes(page.titleRes),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = XiaomiTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle (login_layout_guide_item: @id/subTitle - 12sp Regular)
            Text(
                text = stringRes(page.subtitleRes),
                fontSize = 14.sp,
                color = XiaomiTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Official Illustration (login_layout_guide_item: @id/imageView - 360dp)
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(page.imageRes),
                    contentDescription = stringRes(page.titleRes),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Page Indicator Dots (MIUI Style)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 28.dp)
        ) {
            for (i in pages.indices) {
                val isSelected = i == currentPage
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 18.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) XiaomiCyan else XiaomiCardHover)
                        .clickable { currentPage = i }
                )
            }
        }

        // Bottom Bar (login_activity_guide: skip and next/start)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Skip button (hidden on last page)
            if (!isLastPage) {
                TextButton(
                    onClick = { finishGuide() },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = stringRes("login_guide_skip"),
                        color = XiaomiTextMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(60.dp))
            }

            // Next / Start button
            Button(
                onClick = {
                    if (isLastPage) {
                        finishGuide()
                    } else {
                        currentPage++
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = XiaomiCyan,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (isLastPage) stringRes("login_guide_start") else stringRes("login_guide_next"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
