package com.example.docscanner.model

enum class OcrLanguage(
    val displayName: String,
    val nativeName: String,
    val badge: String,
    val flagEmoji: String = "🌐",
    val iosRecognitionCode: String = "en-US"
) {
    LATIN("English / Latin", "English, Spanish, French, German, etc.", "EN/LATIN", "🌐", "en-US"),
    DEVANAGARI("Hindi / Devanagari", "हिन्दी, मराठी, संस्कृत", "HI/DEV", "🇮🇳", "hi-IN"),
    CHINESE("Chinese", "中文 (简体 / 繁體)", "ZH/CN", "🇨🇳", "zh-Hans"),
    JAPANESE("Japanese", "日本語 (漢字 / かな)", "JA/JP", "🇯🇵", "ja-JP"),
    KOREAN("Korean", "한국어 (한글)", "KO/KR", "🇰🇷", "ko-KR");

    companion object {
        fun fromString(value: String): OcrLanguage {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LATIN
        }
    }
}

