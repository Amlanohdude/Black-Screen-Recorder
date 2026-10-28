package com.example.covers

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class CoverPresetId {
    PURE_BLACK,
    CLOCK,
    ANALOG_CLOCK,
    LOCK_SCREEN,
    DIM_CLOCK,
    SYSTEM_STATUS,
    DASHCAM_HUD,
    TIMER,
    MINIMAL,
    CUSTOM
}

enum class ClockMode(val displayName: String) {
    DIGITAL("Digital"),
    ANALOG("Analog"),
    HYBRID("Hybrid")
}

enum class ClockStyle(val displayName: String) {
    DIGITAL("Digital"),
    THIN("Thin"),
    MINIMAL("Minimal"),
    BOLD("Bold"),
    RETRO("Retro")
}

enum class AnalogClockStyle(val displayName: String) {
    MODERN("Modern"),
    CLASSIC("Classic"),
    MINIMAL("Minimal"),
    DOTS("Dots"),
    ROMAN("Roman")
}

enum class NumeralStyle(val displayName: String) {
    NONE("None"),
    ARABIC("Arabic (12, 3, 6, 9)"),
    ROMAN("Roman (XII, III, VI, IX)"),
    DOTS("Dots")
}

enum class HandThickness(val displayName: String, val hourMultiplier: Float, val minuteMultiplier: Float) {
    THIN("Thin", 0.7f, 0.7f),
    MEDIUM("Medium", 1.0f, 1.0f),
    BOLD("Bold", 1.4f, 1.4f)
}

enum class DateFormatStyle(val displayName: String, val pattern: String) {
    DAY_MONTH_YEAR("04 OCT 2026", "dd MMM yyyy"),
    MONTH_DAY_YEAR("OCT 04, 2026", "MMM dd, yyyy"),
    SLASH_NUMERIC("04/10/2026", "dd/MM/yyyy"),
    FULL_DAY("Sunday, October 4", "EEEE, MMMM d"),
    FULL("Sunday, October 4", "EEEE, MMMM d"),
    SHORT("04/10/2026", "dd/MM/yyyy"),
    MEDIUM("04 OCT 2026", "dd MMM yyyy")
}

enum class ControlGesture(val displayName: String) {
    SINGLE_TAP("Single Tap"),
    DOUBLE_TAP("Double Tap"),
    LONG_PRESS("Long Press")
}

enum class CoverBackground(val displayName: String) {
    PURE_BLACK("Pure Black"),
    VERY_DARK_GRAY("Very Dark Gray"),
    DEEP_NAVY("Deep Navy"),
    GRADIENT("Gradient"),
    CUSTOM_COLOR("Custom Color"),
    IMAGE("Wallpaper / Photo")
}

enum class ElementVerticalPosition(val displayName: String) {
    TOP("Top"),
    CENTER("Center"),
    BOTTOM("Bottom")
}

enum class ElementHorizontalAlignment(val displayName: String) {
    START("Left"),
    CENTER("Center"),
    END("Right")
}

enum class GradientDirection(val displayName: String) {
    TOP_TO_BOTTOM("Top to Bottom"),
    LEFT_TO_RIGHT("Left to Right"),
    DIAGONAL_TR("Diagonal (Top-Right)"),
    DIAGONAL_TL("Diagonal (Top-Left)")
}

enum class VignetteLevel(val displayName: String, val intensity: Float) {
    OFF("Off", 0f),
    LOW("Subtle", 0.35f),
    MEDIUM("Medium", 0.65f),
    HIGH("Heavy", 0.95f)
}

enum class BlurLevel(val displayName: String, val radiusDp: Float) {
    NONE("None", 0f),
    LOW("Subtle (8dp)", 8f),
    MEDIUM("Medium (16dp)", 16f),
    HIGH("Heavy (32dp)", 32f)
}

enum class SelectedElement {
    CLOCK,
    DATE,
    BATTERY
}

enum class DesignerTab(val title: String) {
    ELEMENTS("Elements"),
    BACKGROUND("Background"),
    CLOCK_STYLE("Style"),
    LAYOUT("Layout")
}

data class ElementPlacement(
    val vertical: ElementVerticalPosition = ElementVerticalPosition.CENTER,
    val horizontal: ElementHorizontalAlignment = ElementHorizontalAlignment.CENTER
)

data class ClockElementConfig(
    val show: Boolean = true,
    val mode: ClockMode = ClockMode.DIGITAL,
    val is24Hour: Boolean = true,
    val style: ClockStyle = ClockStyle.DIGITAL,
    val analogStyle: AnalogClockStyle = AnalogClockStyle.MODERN,
    val numeralStyle: NumeralStyle = NumeralStyle.ARABIC,
    val showTickMarks: Boolean = true,
    val showClockFace: Boolean = false,
    val handThickness: HandThickness = HandThickness.MEDIUM,
    val sizeSp: Int = 56,
    val opacity: Float = 0.9f,
    val colorHex: String = CoverElementColors.WHITE,
    val verticalPosition: ElementVerticalPosition = ElementVerticalPosition.CENTER,
    val horizontalAlignment: ElementHorizontalAlignment = ElementHorizontalAlignment.CENTER,
    val yOffsetDp: Int = 0,
    val showSecondsHand: Boolean = false,
    val hybridDigitalSizeSp: Int = 32,
    val hybridSpacingDp: Int = 8
)

data class DateElementConfig(
    val show: Boolean = false,
    val format: DateFormatStyle = DateFormatStyle.DAY_MONTH_YEAR,
    val sizeSp: Int = 15,
    val opacity: Float = 0.8f,
    val colorHex: String = CoverElementColors.WHITE,
    val verticalPosition: ElementVerticalPosition = ElementVerticalPosition.CENTER,
    val horizontalAlignment: ElementHorizontalAlignment = ElementHorizontalAlignment.CENTER,
    val yOffsetDp: Int = 0
)

data class BatteryElementConfig(
    val show: Boolean = false,
    val showIcon: Boolean = true,
    val showPercent: Boolean = true,
    val opacity: Float = 0.8f,
    val colorHex: String = CoverElementColors.WHITE,
    val sizeSp: Int = 13,
    val verticalPosition: ElementVerticalPosition = ElementVerticalPosition.TOP,
    val horizontalAlignment: ElementHorizontalAlignment = ElementHorizontalAlignment.END,
    val yOffsetDp: Int = 0
)

data class BackgroundConfig(
    val type: CoverBackground = CoverBackground.PURE_BLACK,
    val brightnessPercent: Int = 10,
    val customColorHex: String = "#000000",
    val opacity: Float = 1.0f,
    val gradientStartHex: String = "#000000",
    val gradientEndHex: String = "#000000",
    val gradientDirection: GradientDirection = GradientDirection.TOP_TO_BOTTOM,
    val imagePath: String? = null,
    val imageDimmingPercent: Int = 50,
    val imageCropScale: Boolean = false,
    val blurLevel: BlurLevel = BlurLevel.NONE,
    val vignetteLevel: VignetteLevel = VignetteLevel.OFF
)

object CoverElementColors {
    const val WHITE = "#FFFFFF"
    const val GRAY = "#AAAAAA"
    const val RED = "#FF4444"
    const val BLUE = "#4488FF"
    const val GREEN = "#44FF88"
    const val YELLOW = "#FFEE44"
    const val ORANGE = "#FFAA33"
    const val PURPLE = "#AA44FF"
    const val CYAN = "#44FFEE"

    fun parseColor(hex: String, fallback: Color = Color.White): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Throwable) {
            fallback
        }
    }
}

data class CoverConfig(
    val id: String = UUID.randomUUID().toString(),
    val presetId: CoverPresetId = CoverPresetId.CUSTOM,
    val name: String = "Custom",
    val isCustom: Boolean = true,
    val showClock: Boolean = true,
    val clockMode: ClockMode = ClockMode.DIGITAL,
    val clockIs24Hour: Boolean = true,
    val clockStyle: ClockStyle = ClockStyle.DIGITAL,
    val analogStyle: AnalogClockStyle = AnalogClockStyle.MODERN,
    val numeralStyle: NumeralStyle = NumeralStyle.ARABIC,
    val showTickMarks: Boolean = true,
    val showClockFace: Boolean = false,
    val handThickness: HandThickness = HandThickness.MEDIUM,
    val clockSizeSp: Int = 56,
    val clockOpacity: Float = 0.9f,
    val clockColorHex: String = CoverElementColors.WHITE,
    val clockVerticalPosition: ElementVerticalPosition = ElementVerticalPosition.CENTER,
    val clockHorizontalAlignment: ElementHorizontalAlignment = ElementHorizontalAlignment.CENTER,
    val clockYOffsetDp: Int = 0,
    val clockShowSeconds: Boolean = false,
    val hybridDigitalSizeSp: Int = 32,
    val hybridSpacingDp: Int = 8,
    val showDate: Boolean = false,
    val dateFormat: DateFormatStyle = DateFormatStyle.DAY_MONTH_YEAR,
    val dateOpacity: Float = 0.8f,
    val dateColorHex: String = CoverElementColors.WHITE,
    val dateSizeSp: Int = 15,
    val dateVerticalPosition: ElementVerticalPosition = ElementVerticalPosition.CENTER,
    val dateHorizontalAlignment: ElementHorizontalAlignment = ElementHorizontalAlignment.CENTER,
    val dateYOffsetDp: Int = 0,
    val showBattery: Boolean = false,
    val batteryShowIcon: Boolean = true,
    val batteryShowPercent: Boolean = true,
    val batteryOpacity: Float = 0.8f,
    val batteryColorHex: String = CoverElementColors.WHITE,
    val batterySizeSp: Int = 13,
    val batteryVerticalPosition: ElementVerticalPosition = ElementVerticalPosition.TOP,
    val batteryHorizontalAlignment: ElementHorizontalAlignment = ElementHorizontalAlignment.END,
    val batteryYOffsetDp: Int = 0,
    val background: CoverBackground = CoverBackground.PURE_BLACK,
    val brightnessPercent: Int = 10,
    val customBackgroundColorHex: String = "#000000",
    val backgroundOpacity: Float = 1.0f,
    val gradientStartHex: String = "#000000",
    val gradientEndHex: String = "#000000",
    val gradientDirection: GradientDirection = GradientDirection.TOP_TO_BOTTOM,
    val imagePath: String? = null,
    val imageDimmingPercent: Int = 50,
    val imageCropScale: Boolean = false,
    val blurLevel: BlurLevel = BlurLevel.NONE,
    val vignetteLevel: VignetteLevel = VignetteLevel.OFF,
    val showRecordingTimer: Boolean = false,
    val controlGesture: ControlGesture = ControlGesture.SINGLE_TAP,
    val burnInProtection: Boolean = true
) {
    val clock: ClockElementConfig
        get() = ClockElementConfig(
            show = showClock,
            mode = clockMode,
            is24Hour = clockIs24Hour,
            style = clockStyle,
            analogStyle = analogStyle,
            numeralStyle = numeralStyle,
            showTickMarks = showTickMarks,
            showClockFace = showClockFace,
            handThickness = handThickness,
            sizeSp = clockSizeSp,
            opacity = clockOpacity,
            colorHex = clockColorHex,
            verticalPosition = clockVerticalPosition,
            horizontalAlignment = clockHorizontalAlignment,
            yOffsetDp = clockYOffsetDp,
            showSecondsHand = clockShowSeconds,
            hybridDigitalSizeSp = hybridDigitalSizeSp,
            hybridSpacingDp = hybridSpacingDp
        )

    val date: DateElementConfig
        get() = DateElementConfig(
            show = showDate,
            format = dateFormat,
            sizeSp = dateSizeSp,
            opacity = dateOpacity,
            colorHex = dateColorHex,
            verticalPosition = dateVerticalPosition,
            horizontalAlignment = dateHorizontalAlignment,
            yOffsetDp = dateYOffsetDp
        )

    val battery: BatteryElementConfig
        get() = BatteryElementConfig(
            show = showBattery,
            showIcon = batteryShowIcon,
            showPercent = batteryShowPercent,
            opacity = batteryOpacity,
            colorHex = batteryColorHex,
            sizeSp = batterySizeSp,
            verticalPosition = batteryVerticalPosition,
            horizontalAlignment = batteryHorizontalAlignment,
            yOffsetDp = batteryYOffsetDp
        )

    val backgroundConfig: BackgroundConfig
        get() = BackgroundConfig(
            type = background,
            brightnessPercent = brightnessPercent,
            customColorHex = customBackgroundColorHex,
            opacity = backgroundOpacity,
            gradientStartHex = gradientStartHex,
            gradientEndHex = gradientEndHex,
            gradientDirection = gradientDirection,
            imagePath = imagePath,
            imageDimmingPercent = imageDimmingPercent,
            imageCropScale = imageCropScale,
            blurLevel = blurLevel,
            vignetteLevel = vignetteLevel
        )

    fun withClock(newClock: ClockElementConfig): CoverConfig = copy(
        showClock = newClock.show,
        clockMode = newClock.mode,
        clockIs24Hour = newClock.is24Hour,
        clockStyle = newClock.style,
        analogStyle = newClock.analogStyle,
        numeralStyle = newClock.numeralStyle,
        showTickMarks = newClock.showTickMarks,
        showClockFace = newClock.showClockFace,
        handThickness = newClock.handThickness,
        clockSizeSp = newClock.sizeSp,
        clockOpacity = newClock.opacity,
        clockColorHex = newClock.colorHex,
        clockVerticalPosition = newClock.verticalPosition,
        clockHorizontalAlignment = newClock.horizontalAlignment,
        clockYOffsetDp = newClock.yOffsetDp,
        clockShowSeconds = newClock.showSecondsHand,
        hybridDigitalSizeSp = newClock.hybridDigitalSizeSp,
        hybridSpacingDp = newClock.hybridSpacingDp
    )

    fun withDate(newDate: DateElementConfig): CoverConfig = copy(
        showDate = newDate.show,
        dateFormat = newDate.format,
        dateSizeSp = newDate.sizeSp,
        dateOpacity = newDate.opacity,
        dateColorHex = newDate.colorHex,
        dateVerticalPosition = newDate.verticalPosition,
        dateHorizontalAlignment = newDate.horizontalAlignment,
        dateYOffsetDp = newDate.yOffsetDp
    )

    fun withBattery(newBattery: BatteryElementConfig): CoverConfig = copy(
        showBattery = newBattery.show,
        batteryShowIcon = newBattery.showIcon,
        batteryShowPercent = newBattery.showPercent,
        batteryOpacity = newBattery.opacity,
        batteryColorHex = newBattery.colorHex,
        batterySizeSp = newBattery.sizeSp,
        batteryVerticalPosition = newBattery.verticalPosition,
        batteryHorizontalAlignment = newBattery.horizontalAlignment,
        batteryYOffsetDp = newBattery.yOffsetDp
    )

    fun withBackground(newBg: BackgroundConfig): CoverConfig = copy(
        background = newBg.type,
        brightnessPercent = newBg.brightnessPercent,
        customBackgroundColorHex = newBg.customColorHex,
        backgroundOpacity = newBg.opacity,
        gradientStartHex = newBg.gradientStartHex,
        gradientEndHex = newBg.gradientEndHex,
        gradientDirection = newBg.gradientDirection,
        imagePath = newBg.imagePath,
        imageDimmingPercent = newBg.imageDimmingPercent,
        imageCropScale = newBg.imageCropScale,
        blurLevel = newBg.blurLevel,
        vignetteLevel = newBg.vignetteLevel
    )

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("presetId", presetId.name)
        json.put("name", name)
        json.put("isCustom", isCustom)
        json.put("showClock", showClock)
        json.put("clockMode", clockMode.name)
        json.put("clockIs24Hour", clockIs24Hour)
        json.put("clockStyle", clockStyle.name)
        json.put("analogStyle", analogStyle.name)
        json.put("numeralStyle", numeralStyle.name)
        json.put("showTickMarks", showTickMarks)
        json.put("showClockFace", showClockFace)
        json.put("handThickness", handThickness.name)
        json.put("clockSizeSp", clockSizeSp)
        json.put("clockOpacity", clockOpacity.toDouble())
        json.put("clockColorHex", clockColorHex)
        json.put("clockVerticalPosition", clockVerticalPosition.name)
        json.put("clockHorizontalAlignment", clockHorizontalAlignment.name)
        json.put("clockYOffsetDp", clockYOffsetDp)
        json.put("clockShowSeconds", clockShowSeconds)
        json.put("hybridDigitalSizeSp", hybridDigitalSizeSp)
        json.put("hybridSpacingDp", hybridSpacingDp)
        json.put("showDate", showDate)
        json.put("dateFormat", dateFormat.name)
        json.put("dateOpacity", dateOpacity.toDouble())
        json.put("dateColorHex", dateColorHex)
        json.put("dateSizeSp", dateSizeSp)
        json.put("dateVerticalPosition", dateVerticalPosition.name)
        json.put("dateHorizontalAlignment", dateHorizontalAlignment.name)
        json.put("dateYOffsetDp", dateYOffsetDp)
        json.put("showBattery", showBattery)
        json.put("batteryShowIcon", batteryShowIcon)
        json.put("batteryShowPercent", batteryShowPercent)
        json.put("batteryOpacity", batteryOpacity.toDouble())
        json.put("batteryColorHex", batteryColorHex)
        json.put("batterySizeSp", batterySizeSp)
        json.put("batteryVerticalPosition", batteryVerticalPosition.name)
        json.put("batteryHorizontalAlignment", batteryHorizontalAlignment.name)
        json.put("batteryYOffsetDp", batteryYOffsetDp)
        json.put("background", background.name)
        json.put("brightnessPercent", brightnessPercent)
        json.put("customBackgroundColorHex", customBackgroundColorHex)
        json.put("backgroundOpacity", backgroundOpacity.toDouble())
        json.put("gradientStartHex", gradientStartHex)
        json.put("gradientEndHex", gradientEndHex)
        json.put("gradientDirection", gradientDirection.name)
        json.put("imagePath", imagePath)
        json.put("imageDimmingPercent", imageDimmingPercent)
        json.put("imageCropScale", imageCropScale)
        json.put("blurLevel", blurLevel.name)
        json.put("vignetteLevel", vignetteLevel.name)
        json.put("showRecordingTimer", showRecordingTimer)
        json.put("controlGesture", controlGesture.name)
        json.put("burnInProtection", burnInProtection)
        return json
    }

    companion object {
        val PURE_BLACK_PRESET = CoverConfig(
            id = "preset_pure_black",
            presetId = CoverPresetId.PURE_BLACK,
            name = "Pure Black",
            isCustom = false,
            showClock = false,
            showDate = false,
            showBattery = false,
            background = CoverBackground.PURE_BLACK,
            brightnessPercent = 1
        )

        val CLOCK_PRESET = CoverConfig(
            id = "preset_clock",
            presetId = CoverPresetId.CLOCK,
            name = "Clock",
            isCustom = false,
            showClock = true,
            clockMode = ClockMode.DIGITAL,
            clockStyle = ClockStyle.DIGITAL,
            clockSizeSp = 56,
            clockOpacity = 0.9f,
            showDate = true,
            dateFormat = DateFormatStyle.FULL_DAY,
            dateOpacity = 0.7f,
            dateVerticalPosition = ElementVerticalPosition.CENTER,
            dateHorizontalAlignment = ElementHorizontalAlignment.CENTER,
            dateYOffsetDp = 38,
            showBattery = false,
            background = CoverBackground.PURE_BLACK,
            brightnessPercent = 25
        )

        val ANALOG_CLOCK_PRESET = CoverConfig(
            id = "preset_analog_clock",
            presetId = CoverPresetId.ANALOG_CLOCK,
            name = "Analog Clock",
            isCustom = false,
            showClock = true,
            clockMode = ClockMode.ANALOG,
            analogStyle = AnalogClockStyle.MODERN,
            numeralStyle = NumeralStyle.ARABIC,
            showTickMarks = true,
            showClockFace = true,
            clockSizeSp = 180,
            clockOpacity = 0.9f,
            showDate = true,
            dateFormat = DateFormatStyle.DAY_MONTH_YEAR,
            dateOpacity = 0.75f,
            dateVerticalPosition = ElementVerticalPosition.CENTER,
            dateHorizontalAlignment = ElementHorizontalAlignment.CENTER,
            dateYOffsetDp = 110,
            showBattery = false,
            background = CoverBackground.PURE_BLACK,
            brightnessPercent = 25
        )

        val LOCK_SCREEN_PRESET = CoverConfig(
            id = "preset_lock_screen",
            presetId = CoverPresetId.LOCK_SCREEN,
            name = "Lock Style",
            isCustom = false,
            showClock = true,
            clockMode = ClockMode.DIGITAL,
            clockStyle = ClockStyle.THIN,
            clockSizeSp = 64,
            clockOpacity = 0.95f,
            clockVerticalPosition = ElementVerticalPosition.TOP,
            clockHorizontalAlignment = ElementHorizontalAlignment.START,
            clockYOffsetDp = 30,
            showDate = true,
            dateFormat = DateFormatStyle.FULL_DAY,
            dateOpacity = 0.85f,
            dateVerticalPosition = ElementVerticalPosition.TOP,
            dateHorizontalAlignment = ElementHorizontalAlignment.START,
            dateYOffsetDp = 100,
            showBattery = true,
            batteryShowIcon = true,
            batteryShowPercent = true,
            batteryOpacity = 0.85f,
            batteryVerticalPosition = ElementVerticalPosition.TOP,
            batteryHorizontalAlignment = ElementHorizontalAlignment.END,
            batteryYOffsetDp = 10,
            background = CoverBackground.VERY_DARK_GRAY,
            brightnessPercent = 40
        )

        val DIM_CLOCK_PRESET = CoverConfig(
            id = "preset_dim_clock",
            presetId = CoverPresetId.DIM_CLOCK,
            name = "Dim Clock",
            isCustom = false,
            showClock = true,
            clockMode = ClockMode.DIGITAL,
            clockStyle = ClockStyle.DIGITAL,
            clockSizeSp = 50,
            clockOpacity = 0.22f,
            showDate = false,
            showBattery = false,
            background = CoverBackground.PURE_BLACK,
            brightnessPercent = 1
        )

        val SYSTEM_STATUS_PRESET = CoverConfig(
            id = "preset_system_status",
            presetId = CoverPresetId.SYSTEM_STATUS,
            name = "System Status",
            isCustom = false,
            showClock = true,
            clockMode = ClockMode.DIGITAL,
            clockStyle = ClockStyle.DIGITAL,
            showDate = true,
            showBattery = true,
            background = CoverBackground.PURE_BLACK,
            brightnessPercent = 25
        )

        val DASHCAM_HUD_PRESET = CoverConfig(
            id = "preset_dashcam_hud",
            presetId = CoverPresetId.DASHCAM_HUD,
            name = "Dashcam HUD",
            isCustom = false,
            showClock = true,
            clockMode = ClockMode.DIGITAL,
            clockStyle = ClockStyle.BOLD,
            showDate = true,
            showBattery = false,
            background = CoverBackground.PURE_BLACK,
            brightnessPercent = 35
        )

        val TIMER_PRESET = CoverConfig(
            id = "preset_timer",
            presetId = CoverPresetId.TIMER,
            name = "Stopwatch / Timer",
            isCustom = false,
            showClock = false,
            showDate = false,
            showBattery = false,
            background = CoverBackground.PURE_BLACK,
            brightnessPercent = 25
        )

        val ALL_PRESETS = listOf(
            PURE_BLACK_PRESET,
            CLOCK_PRESET,
            ANALOG_CLOCK_PRESET,
            LOCK_SCREEN_PRESET,
            DIM_CLOCK_PRESET,
            SYSTEM_STATUS_PRESET,
            DASHCAM_HUD_PRESET,
            TIMER_PRESET
        )

        fun forPreset(presetId: CoverPresetId): CoverConfig {
            return when (presetId) {
                CoverPresetId.PURE_BLACK -> PURE_BLACK_PRESET
                CoverPresetId.CLOCK -> CLOCK_PRESET
                CoverPresetId.ANALOG_CLOCK -> ANALOG_CLOCK_PRESET
                CoverPresetId.LOCK_SCREEN -> LOCK_SCREEN_PRESET
                CoverPresetId.DIM_CLOCK -> DIM_CLOCK_PRESET
                CoverPresetId.SYSTEM_STATUS -> SYSTEM_STATUS_PRESET
                CoverPresetId.DASHCAM_HUD -> DASHCAM_HUD_PRESET
                CoverPresetId.TIMER -> TIMER_PRESET
                CoverPresetId.CUSTOM,
                CoverPresetId.MINIMAL -> createDefaultCustomCover()
            }
        }

        fun createDefaultCustomCover(): CoverConfig {
            return CoverConfig(
                id = UUID.randomUUID().toString(),
                presetId = CoverPresetId.CUSTOM,
                name = "My Custom Cover",
                isCustom = true
            )
        }

        fun listToJson(list: List<CoverConfig>): String {
            val arr = JSONArray()
            for (item in list) {
                arr.put(item.toJson())
            }
            return arr.toString()
        }

        fun listFromJson(jsonStr: String?): List<CoverConfig> {
            if (jsonStr.isNullOrBlank()) return emptyList()
            val list = mutableListOf<CoverConfig>()
            return try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(fromJson(obj))
                }
                list
            } catch (_: Throwable) {
                emptyList()
            }
        }

        fun fromJson(json: JSONObject): CoverConfig {
            return try {
                CoverConfig(
                    id = json.optString("id", UUID.randomUUID().toString()),
                    presetId = CoverPresetId.valueOf(json.optString("presetId", "CUSTOM")),
                    name = json.optString("name", "Custom"),
                    isCustom = json.optBoolean("isCustom", true),
                    showClock = json.optBoolean("showClock", true),
                    clockMode = ClockMode.valueOf(json.optString("clockMode", "DIGITAL")),
                    clockIs24Hour = json.optBoolean("clockIs24Hour", true),
                    clockStyle = ClockStyle.valueOf(json.optString("clockStyle", "DIGITAL")),
                    analogStyle = AnalogClockStyle.valueOf(json.optString("analogStyle", "MODERN")),
                    numeralStyle = NumeralStyle.valueOf(json.optString("numeralStyle", "ARABIC")),
                    showTickMarks = json.optBoolean("showTickMarks", true),
                    showClockFace = json.optBoolean("showClockFace", false),
                    handThickness = HandThickness.valueOf(json.optString("handThickness", "MEDIUM")),
                    clockSizeSp = json.optInt("clockSizeSp", 56),
                    clockOpacity = json.optDouble("clockOpacity", 0.9).toFloat(),
                    clockColorHex = json.optString("clockColorHex", CoverElementColors.WHITE),
                    clockVerticalPosition = ElementVerticalPosition.valueOf(json.optString("clockVerticalPosition", "CENTER")),
                    clockHorizontalAlignment = ElementHorizontalAlignment.valueOf(json.optString("clockHorizontalAlignment", "CENTER")),
                    clockYOffsetDp = json.optInt("clockYOffsetDp", 0),
                    clockShowSeconds = json.optBoolean("clockShowSeconds", false),
                    hybridDigitalSizeSp = json.optInt("hybridDigitalSizeSp", 32),
                    hybridSpacingDp = json.optInt("hybridSpacingDp", 8),
                    showDate = json.optBoolean("showDate", false),
                    dateFormat = DateFormatStyle.valueOf(json.optString("dateFormat", "DAY_MONTH_YEAR")),
                    dateOpacity = json.optDouble("dateOpacity", 0.8).toFloat(),
                    dateColorHex = json.optString("dateColorHex", CoverElementColors.WHITE),
                    dateSizeSp = json.optInt("dateSizeSp", 15),
                    dateVerticalPosition = ElementVerticalPosition.valueOf(json.optString("dateVerticalPosition", "CENTER")),
                    dateHorizontalAlignment = ElementHorizontalAlignment.valueOf(json.optString("dateHorizontalAlignment", "CENTER")),
                    dateYOffsetDp = json.optInt("dateYOffsetDp", 0),
                    showBattery = json.optBoolean("showBattery", false),
                    batteryShowIcon = json.optBoolean("batteryShowIcon", true),
                    batteryShowPercent = json.optBoolean("batteryShowPercent", true),
                    batteryOpacity = json.optDouble("batteryOpacity", 0.8).toFloat(),
                    batteryColorHex = json.optString("batteryColorHex", CoverElementColors.WHITE),
                    batterySizeSp = json.optInt("batterySizeSp", 13),
                    batteryVerticalPosition = ElementVerticalPosition.valueOf(json.optString("batteryVerticalPosition", "TOP")),
                    batteryHorizontalAlignment = ElementHorizontalAlignment.valueOf(json.optString("batteryHorizontalAlignment", "END")),
                    batteryYOffsetDp = json.optInt("batteryYOffsetDp", 0),
                    background = CoverBackground.valueOf(json.optString("background", "PURE_BLACK")),
                    brightnessPercent = json.optInt("brightnessPercent", 10),
                    customBackgroundColorHex = json.optString("customBackgroundColorHex", "#000000"),
                    backgroundOpacity = json.optDouble("backgroundOpacity", 1.0).toFloat(),
                    gradientStartHex = json.optString("gradientStartHex", "#000000"),
                    gradientEndHex = json.optString("gradientEndHex", "#000000"),
                    gradientDirection = GradientDirection.valueOf(json.optString("gradientDirection", "TOP_TO_BOTTOM")),
                    imagePath = if (json.has("imagePath") && !json.isNull("imagePath")) json.getString("imagePath") else null,
                    imageDimmingPercent = json.optInt("imageDimmingPercent", 50),
                    imageCropScale = json.optBoolean("imageCropScale", false),
                    blurLevel = BlurLevel.valueOf(json.optString("blurLevel", "NONE")),
                    vignetteLevel = VignetteLevel.valueOf(json.optString("vignetteLevel", "OFF")),
                    showRecordingTimer = json.optBoolean("showRecordingTimer", false),
                    controlGesture = ControlGesture.valueOf(json.optString("controlGesture", "SINGLE_TAP")),
                    burnInProtection = json.optBoolean("burnInProtection", true)
                )
            } catch (_: Throwable) {
                PURE_BLACK_PRESET
            }
        }
    }
}
