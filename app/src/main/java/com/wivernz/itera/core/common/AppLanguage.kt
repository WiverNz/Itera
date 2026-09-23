package com.wivernz.itera.core.common
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.LocaleListCompat
import java.util.Locale
/** AppCompat owns locale persistence; there is deliberately no DataStore mirror. */
object AppLanguage {
    val tags = listOf("", "en", "ru", "de", "es")
    fun current(): String = AppCompatDelegate.getApplicationLocales()
        .toLanguageTags()
        .substringBefore('-')
    fun set(tag: String) {
        require(tag in tags)
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(
                    tag
                )
            }
        )
    }
    fun nativeName(tag: String): String = localName(tag, Locale.forLanguageTag(tag))
    fun localName(tag: String, inLocale: Locale): String = Locale.forLanguageTag(tag)
        .getDisplayLanguage(inLocale)
        .replaceFirstChar {
            it.titlecase(inLocale)
        }
}

@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
