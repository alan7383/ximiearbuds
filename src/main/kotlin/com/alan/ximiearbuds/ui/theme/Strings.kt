package com.alan.ximiearbuds.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import kotlinx.serialization.json.Json
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

enum class AppLanguage(val code: String, val displayName: String) {
    FR("fr", "Français"),
    EN("en", "English"),
    ES("es", "Español"),
    DE("de", "Deutsch"),
    IT("it", "Italiano"),
    RU("ru", "Русский"),
    ZH_CN("zh_CN", "简体中文"),
    ZH_TW("zh_TW", "繁體中文"),
    JA("ja", "日本語"),
    KO("ko", "한국어"),
    PT_BR("pt_BR", "Português"),
    PL("pl", "Polski"),
    TR("tr", "Türkçe"),
    NL("nl", "Nederlands"),
    UK("uk", "Українська"),
    AR("ar", "العربية"),
    TH("th", "ไทย"),
    VI("vi", "Tiếng Việt"),
    ID("id", "Indonesia"),
    HI("hi", "हिन्दी"),
    HU("hu", "Magyar"),
    FI("fi", "Suomi"),
    SV("sv", "Svenska")
}

val LocalStrings = staticCompositionLocalOf { StringsManager.getStrings(AppLanguage.FR) }

object StringsManager {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val cache = ConcurrentHashMap<String, Map<String, String>>()

    init {
        // Preload base English and French
        loadLanguage("en")
        loadLanguage("fr")
    }

    private fun loadLanguage(code: String): Map<String, String> {
        return cache.computeIfAbsent(code) {
            val path = "strings/strings_$code.json"
            try {
                val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(path)
                if (stream != null) {
                    val content = InputStreamReader(stream, StandardCharsets.UTF_8).use { it.readText() }
                    json.decodeFromString<Map<String, String>>(content)
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                System.err.println("Error loading language $code: ${e.message}")
                emptyMap()
            }
        }
    }

    fun getStrings(lang: AppLanguage): Strings {
        val activeMap = loadLanguage(lang.code)
        val fallbackMap = if (lang.code != "en") loadLanguage("en") else emptyMap()
        return Strings(activeMap, fallbackMap)
    }
}

class Strings(
    private val current: Map<String, String>,
    private val fallback: Map<String, String>
) {
    operator fun get(key: String, vararg args: Any): String {
        val raw = current[key] ?: fallback[key] ?: key
        return if (args.isNotEmpty()) {
            try {
                String.format(raw, *args)
            } catch (_: Exception) {
                raw
            }
        } else {
            raw
        }
    }
}

@Composable
fun stringRes(key: String, vararg args: Any): String {
    val strings = LocalStrings.current
    return strings.get(key, *args)
}

/**
 * Parses official Xiaomi HTML formatted string resources (like `<a href=%1$s>Text</a>`)
 * into Compose AnnotatedString with clickable URL annotations.
 */
fun parseHtmlLinks(
    html: String,
    linkColor: Color = Color(0xFF1F93FF)
): AnnotatedString {
    val regex = Regex("""<a\s+href=['"]?([^'">]+)['"]?>(.*?)</a>""")
    return buildAnnotatedString {
        var lastIndex = 0
        val matches = regex.findAll(html)
        for (match in matches) {
            val range = match.range
            if (range.first > lastIndex) {
                append(html.substring(lastIndex, range.first))
            }
            val url = match.groupValues[1]
            val linkText = match.groupValues[2]
            pushStringAnnotation(tag = "URL", annotation = url)
            pushStringAnnotation(tag = "AGREEMENT", annotation = url)
            withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Medium)) {
                append(linkText)
            }
            pop()
            pop()
            lastIndex = range.last + 1
        }
        if (lastIndex < html.length) {
            append(html.substring(lastIndex))
        }
    }
}
