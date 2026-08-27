package com.example.docscanner.model

enum class OcrLanguage(
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String,
    val iosRecognitionCode: String
) {
    LATIN("English / Latin", "Latin Script", "🌐", "en-US"),
    DEVANAGARI("Hindi / Devanagari", "हिन्दी / देवनागरी", "🇮🇳", "hi-IN"),
    CHINESE("Chinese", "中文 (Simplified)", "🇨🇳", "zh-Hans"),
    JAPANESE("Japanese", "日本語", "🇯🇵", "ja-JP"),
    KOREAN("Korean", "한국어", "🇰🇷", "ko-KR");

    companion object {
        fun fromString(value: String): OcrLanguage {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LATIN
        }
    }
}
