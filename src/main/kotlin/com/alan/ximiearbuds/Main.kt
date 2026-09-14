package com.alan.ximiearbuds

import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.alan.ximiearbuds.core.device.EarbudsController
import com.alan.ximiearbuds.ui.MainWindow
import com.alan.ximiearbuds.ui.theme.AppLanguage
import com.alan.ximiearbuds.ui.theme.XimiEarbudsTheme
import java.awt.Dimension

fun main() = application {
    val windowState = rememberWindowState(width = 460.dp, height = 860.dp)
    val controller = remember { EarbudsController() }

    var isDarkTheme by remember { mutableStateOf(true) }
    var currentLanguage by remember { mutableStateOf(AppLanguage.FR) }

    Window(
        onCloseRequest = {
            controller.disconnect()
            exitApplication()
        },
        state = windowState,
        title = "Xiaomi Earbuds",
        icon = painterResource("icons/app_icon.png")
    ) {
        window.minimumSize = Dimension(400, 560)

        XimiEarbudsTheme(
            darkTheme = isDarkTheme,
            language = currentLanguage
        ) {
            MainWindow(
                controller = controller,
                isDarkTheme = isDarkTheme,
                currentLanguage = currentLanguage,
                onThemeToggled = { isDarkTheme = !isDarkTheme },
                onLanguageSelected = { currentLanguage = it }
            )
        }
    }
}
