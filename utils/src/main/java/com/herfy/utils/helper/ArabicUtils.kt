package com.herfy.utils.helper

object ArabicUtils {
    fun isArabic(name:String): Boolean {

        var i = 0
        while (i < name.length) {
            val c: Int = name.codePointAt(i)
            if (c in 0x0600..0x06E0) return true
            i += Character.charCount(c)
        }
        return false;
    }
}