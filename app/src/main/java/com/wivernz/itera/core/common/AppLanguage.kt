package com.wivernz.itera.core.common
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.LocaleListCompat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
/** AppCompat owns locale persistence; there is deliberately no DataStore mirror. */
object AppLanguage {
    val tags = listOf("", "en", "ru", "de", "es")
    private val active = MutableStateFlow(Locale.getDefault())

    /**
     * The UI locale. The activity handles locale changes in place (no recreate, no flash), so flows that
     * resolve translated text combine with this to re-resolve it; MainActivity publishes every change.
     */
    val locale: StateFlow<Locale> = active.asStateFlow()
    fun publish(locale: Locale) {
        active.value = locale
    }
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
