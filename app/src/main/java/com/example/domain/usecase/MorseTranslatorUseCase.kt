package com.example.domain.usecase

class MorseTranslatorUseCase {

    private val englishToMorseMap = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.", 'G' to "--.",
        'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.",
        'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-", 'U' to "..-",
        'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--", 'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--",
        '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", '&' to ".-...", ':' to "---...",
        ';' to "-.-.-.", '=' to "-...-", '+' to ".-.-.", '-' to "-....-", '_' to "..--.-",
        '"' to ".-..-.", '$' to "...-..-", '@' to ".--.-."
    )

    private val persianToMorseMap = mapOf(
        'ا' to ".-", 'آ' to ".-", 'ب' to "-...", 'پ' to ".--.", 'ت' to "-", 'ث' to "-.-.",
        'ج' to ".---", 'چ' to "---.", 'ح' to "....", 'خ' to "-..-", 'د' to "-..", 'ذ' to "--..",
        'ر' to ".-.", 'ز' to "---", 'ژ' to "--.-", 'س' to "...", 'ش' to "----", 'ص' to "-.-.",
        'ض' to ".--.", 'ط' to "..-", 'ظ' to "-.--", 'ع' to ".-.-", 'غ' to "--.", 'ف' to "..-.",
        'ق' to "--.-", 'ک' to "-.-", 'گ' to "--.", 'ل' to ".-..", 'م' to "--", 'ن' to "-.",
        'و' to ".--", 'ه' to "..-..", 'ی' to "..",
        '۰' to "-----", '۱' to ".----", '۲' to "..---", '۳' to "...--", '۴' to "....-",
        '۵' to ".....", '۶' to "-....", '۷' to "--...", '۸' to "---..", '۹' to "----.",
        '?' to "..--..", '!' to "-.-.--", '.' to ".-.-.-", '،' to "--..--"
    )

    private val morseToEnglishMap = englishToMorseMap.entries.associate { (k, v) -> v to k }
    private val morseToPersianMap = persianToMorseMap.entries.associate { (k, v) -> v to k }

    fun textToMorse(text: String, isEnglish: Boolean): String {
        val map = if (isEnglish) englishToMorseMap else persianToMorseMap
        return text.uppercase().map { char ->
            if (char == ' ') " " // Word separator
            else map[char]?.let { "$it " } ?: "" // Letter separator
        }.joinToString("").replace("  ", " / ").trim() // Convert double space to /
    }

    fun morseToText(morse: String, isEnglish: Boolean): String {
        val map = if (isEnglish) morseToEnglishMap else morseToPersianMap
        return morse.trim().split(" / ", "   ").joinToString(" ") { word ->
            word.split(" ").mapNotNull { char ->
                map[char]?.toString()
            }.joinToString("")
        }
    }
}
