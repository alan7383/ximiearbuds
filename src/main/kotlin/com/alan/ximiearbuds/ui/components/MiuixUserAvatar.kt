package com.alan.ximiearbuds.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alan.ximiearbuds.core.account.XiaomiAccountClient
import com.alan.ximiearbuds.core.device.DevicePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.io.File

/**
 * 1:1 reproduction of Xiaomi Earbuds header user avatar (`user_avatar_iv`).
 * Displays logged-in user's profile image or default squircle avatar.
 */
@Composable
fun MiuixUserAvatar(
    size: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val isLoggedIn = DevicePreferences.isLoggedIn()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            withContext(Dispatchers.IO) {
                val userId = DevicePreferences.getUserId() ?: ""
                val appDir = File(System.getProperty("user.home"), ".config/ximiearbuds")
                val avatarFile = File(appDir, "avatar_${userId}.jpg")
                if (avatarFile.exists() && avatarFile.length() > 0) {
                    try {
                        val bytes = avatarFile.readBytes()
                        val skiaImage = Image.makeFromEncoded(bytes)
                        avatarBitmap = skiaImage.toComposeImageBitmap()
                    } catch (e: Exception) {
                        avatarBitmap = null
                    }
                } else {
                    val avatarUrl = DevicePreferences.getAvatarAddress()
                    if (!avatarUrl.isNullOrBlank() && userId.isNotEmpty()) {
                        val localPath = XiaomiAccountClient.downloadAndCacheAvatar(userId, avatarUrl)
                        val downloadedFile = if (localPath.isNotEmpty()) File(localPath) else avatarFile
                        if (downloadedFile.exists() && downloadedFile.length() > 0) {
                            try {
                                val bytes = downloadedFile.readBytes()
                                val skiaImage = Image.makeFromEncoded(bytes)
                                avatarBitmap = skiaImage.toComposeImageBitmap()
                            } catch (e: Exception) {
                                avatarBitmap = null
                            }
                        }
                    }
                }
            }
        } else {
            avatarBitmap = null
        }
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = avatarBitmap
        if (isLoggedIn && bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "User Avatar",
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource("drawable/avatar_default.png"),
                contentDescription = "Default Avatar",
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop
            )
        }
    }
}
