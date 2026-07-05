package com.jiucaihua.app.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object AppLocaleManager {
    const val PREFS_NAME = "jiucaihua_prefs"
    const val KEY_LANGUAGE = "app_language"
    const val LANGUAGE_SYSTEM = ""
    const val LANGUAGE_ZH = "zh"
    const val LANGUAGE_EN = "en"

    val supportedLanguages = listOf(LANGUAGE_SYSTEM, LANGUAGE_ZH, LANGUAGE_EN)

    fun wrap(context: Context): Context {
        val languageTag = getLanguageTag(context)
        if (languageTag.isBlank()) {
            return context
        }

        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    fun getLanguageTag(context: Context): String {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANGUAGE_SYSTEM)
            .orEmpty()
            .takeIf { it in supportedLanguages }
            ?: LANGUAGE_SYSTEM
    }
}
