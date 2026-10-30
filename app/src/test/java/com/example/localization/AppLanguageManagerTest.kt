package com.example.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppLanguageManagerTest {

    @Test
    fun getStrings_returnsCorrectLanguageSet() {
        assertEquals("Record", AppLanguageManager.getStrings("en").tabRecord)
        assertEquals("Grabar", AppLanguageManager.getStrings("es").tabRecord)
        assertEquals("Gravar", AppLanguageManager.getStrings("pt").tabRecord)
        assertEquals("Aufnahme", AppLanguageManager.getStrings("de").tabRecord)
        assertEquals("Enregistrer", AppLanguageManager.getStrings("fr").tabRecord)
        assertEquals("रिकॉर्ड", AppLanguageManager.getStrings("hi").tabRecord)
        assertEquals("Rekam", AppLanguageManager.getStrings("in").tabRecord)
        assertEquals("Rekam", AppLanguageManager.getStrings("id").tabRecord)
        assertEquals("録画", AppLanguageManager.getStrings("ja").tabRecord)
        assertEquals("Запись", AppLanguageManager.getStrings("ru").tabRecord)
        assertEquals("تسجيل", AppLanguageManager.getStrings("ar").tabRecord)
    }

    @Test
    fun appLanguage_fromCode_mapsAllSupportedLanguages() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromCode("es"))
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromCode("pt"))
        assertEquals(AppLanguage.GERMAN, AppLanguage.fromCode("de"))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromCode("fr"))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromCode("hi"))
        assertEquals(AppLanguage.INDONESIAN, AppLanguage.fromCode("in"))
        assertEquals(AppLanguage.JAPANESE, AppLanguage.fromCode("ja"))
        assertEquals(AppLanguage.RUSSIAN, AppLanguage.fromCode("ru"))
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromCode("ar"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode("system"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode("unknown_code"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode(null))
    }

    @Test
    fun appStrings_allLanguagesHaveRequiredFields() {
        val languages = listOf("en", "es", "pt", "de", "fr", "hi", "in", "ja", "ru", "ar")
        for (code in languages) {
            val strings = AppLanguageManager.getStrings(code)
            assertNotNull(strings.tabRecord)
            assertNotNull(strings.tabCovers)
            assertNotNull(strings.tabRecordings)
            assertNotNull(strings.tabSettings)
            assertNotNull(strings.language)
            assertNotNull(strings.batteryGuard)
            assertNotNull(strings.hapticFeedback)
            assertNotNull(strings.startRecording)
            assertNotNull(strings.stopRecording)
            assertNotNull(strings.autoDetect)
            assertNotNull(strings.onboarding.title1)
            assertNotNull(strings.recordingUi.holdingToStop)
            assertNotNull(strings.recordingUi.criticallyLowStorageTitle)
            assertNotNull(strings.recordingUi.ok)
            assertNotNull(strings.recordingUi.apply)
            assertNotNull(strings.audioSheet.title)
            assertFalse(strings.tabRecord.isBlank())
            assertFalse(strings.language.isBlank())
            assertFalse(strings.autoDetect.isBlank())
            assertFalse(strings.onboarding.title1.isBlank())
            assertFalse(strings.recordingUi.holdingToStop.isBlank())
            assertFalse(strings.recordingUi.criticallyLowStorageTitle.isBlank())
            assertFalse(strings.recordingUi.ok.isBlank())
            assertFalse(strings.recordingUi.apply.isBlank())
            assertFalse(strings.audioSheet.title.isBlank())
        }
    }

    @Test
    fun autoDetection_returnsValidLanguageAndLabel() {
        val detected = AppLanguageManager.getDetectedLanguage()
        assertNotNull(detected)
        val strings = AppLanguageManager.getStrings("en")
        val label = AppLanguageManager.getAutoDetectLabel(strings)
        assertNotNull(label)
        org.junit.Assert.assertTrue(label.contains("Auto Detect"))
    }
}
