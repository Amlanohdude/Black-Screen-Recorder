package com.example.localization

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object AppLanguageManager {

    /**
     * Resolves the device's true hardware/OS system language code (e.g. "es", "pt", "hi", "en").
     */
    fun getSystemLanguageCode(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Resources.getSystem().configuration.locales[0]?.language?.lowercase()
                    ?: Locale.getDefault().language.lowercase()
            } else {
                @Suppress("DEPRECATION")
                Resources.getSystem().configuration.locale?.language?.lowercase()
                    ?: Locale.getDefault().language.lowercase()
            }
        } catch (_: Exception) {
            "en"
        }
    }

    /**
     * Maps the detected device language to our supported AppLanguage enum.
     */
    fun getDetectedLanguage(): AppLanguage {
        val code = getSystemLanguageCode()
        return when {
            code.startsWith("es") -> AppLanguage.SPANISH
            code.startsWith("pt") -> AppLanguage.PORTUGUESE
            code.startsWith("de") -> AppLanguage.GERMAN
            code.startsWith("fr") -> AppLanguage.FRENCH
            code.startsWith("hi") -> AppLanguage.HINDI
            code.startsWith("in") || code.startsWith("id") -> AppLanguage.INDONESIAN
            code.startsWith("ja") -> AppLanguage.JAPANESE
            code.startsWith("ru") -> AppLanguage.RUSSIAN
            code.startsWith("ar") -> AppLanguage.ARABIC
            else -> AppLanguage.ENGLISH
        }
    }

    /**
     * Returns a human-friendly label showing Auto-Detect and the detected language.
     * e.g., "Auto Detect (Español)"
     */
    fun getAutoDetectLabel(strings: AppStrings): String {
        val detected = getDetectedLanguage()
        return "${strings.autoDetect} (${detected.nativeName})"
    }

    /**
     * Retrieves the localized AppStrings object for the specified language code.
     * Falls back to device system locale if "system", or English as default.
     */
    fun getStrings(languageCode: String?): AppStrings {
        val code = if (languageCode.isNullOrEmpty() || languageCode == "system" || languageCode == "auto") {
            getSystemLanguageCode()
        } else {
            languageCode.lowercase()
        }

        return when {
            code.startsWith("es") -> SpanishStrings
            code.startsWith("pt") -> PortugueseStrings
            code.startsWith("de") -> GermanStrings
            code.startsWith("fr") -> FrenchStrings
            code.startsWith("hi") -> HindiStrings
            code.startsWith("in") || code.startsWith("id") -> IndonesianStrings
            code.startsWith("ja") -> JapaneseStrings
            code.startsWith("ru") -> RussianStrings
            code.startsWith("ar") -> ArabicStrings
            else -> EnglishStrings
        }
    }

    /**
     * Applies the selected language to the Android app context and system LocaleManager.
     */
    fun applyLocale(context: Context, languageCode: String?) {
        try {
            val targetLocale = if (languageCode.isNullOrEmpty() || languageCode == "system" || languageCode == "auto") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Resources.getSystem().configuration.locales[0] ?: Locale.getDefault()
                } else {
                    @Suppress("DEPRECATION")
                    Resources.getSystem().configuration.locale ?: Locale.getDefault()
                }
            } else {
                Locale.forLanguageTag(languageCode)
            }
            Locale.setDefault(targetLocale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(targetLocale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                if (localeManager != null) {
                    if (languageCode.isNullOrEmpty() || languageCode == "system" || languageCode == "auto") {
                        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
                    } else {
                        localeManager.applicationLocales = LocaleList(targetLocale)
                    }
                }
            }
        } catch (_: Exception) {
            // Safe fallback
        }
    }
}
