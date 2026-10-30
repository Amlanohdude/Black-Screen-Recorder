package com.example.localization

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String
) {
    SYSTEM("system", "System Default", "Default"),
    ENGLISH("en", "English", "English"),
    SPANISH("es", "Spanish", "Español"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    GERMAN("de", "German", "Deutsch"),
    FRENCH("fr", "French", "Français"),
    HINDI("hi", "Hindi", "हिन्दी"),
    INDONESIAN("in", "Indonesian", "Bahasa Indonesia"),
    JAPANESE("ja", "Japanese", "日本語"),
    RUSSIAN("ru", "Russian", "Русский"),
    ARABIC("ar", "Arabic", "العربية");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
        }
    }
}
