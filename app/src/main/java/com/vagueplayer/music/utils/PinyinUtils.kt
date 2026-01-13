package com.vagueplayer.music.utils

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType

/**
 * PinyinUtils using Pinyin4j library for robust Chinese character conversion.
 * This replaces the custom PinyinMapData implementation.
 */
object PinyinUtils {

    private val format = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.UPPERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
        vCharType = HanyuPinyinVCharType.WITH_V
    }

    /**
     * Convert input string to a sortable Pinyin key.
     * Format: "Initial|FullPinyinString"
     * Example: "爱你" -> "A|AINI"
     */
    fun toPinyin(input: String): String {
        if (input.isBlank()) return ""
        val s = input.trim()
        
        // Get first letter for section grouping
        val index = getIndexLetter(s)
        
        // Convert entire string to Pinyin for sorting
        val sb = StringBuilder()
        for (c in s) {
            val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format)
            if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                sb.append(pinyinArray[0]) // Use first pronunciation
            } else if (c.isLetterOrDigit()) {
                sb.append(c.uppercaseChar())
            } else {
                sb.append(c)
            }
        }
        
        return "$index|${sb.toString()}"
    }

    /**
     * Get the index letter (A-Z or #) for the first character.
     */
    fun getIndexLetter(text: String): Char {
        if (text.isBlank()) return '#'
        
        val first = text.first()
        
        // Check ASCII letters first
        if (first in 'A'..'Z') return first
        if (first in 'a'..'z') return first.uppercaseChar()
        
        // Check if it's a Chinese character using Pinyin4j
        val pinyinArray = try {
            PinyinHelper.toHanyuPinyinStringArray(first, format)
        } catch (e: Exception) {
            null
        }
        
        if (pinyinArray != null && pinyinArray.isNotEmpty()) {
            val pinyin = pinyinArray[0]
            if (pinyin.isNotEmpty()) {
                return pinyin.first().uppercaseChar()
            }
        }
        
        return '#'
    }
}
