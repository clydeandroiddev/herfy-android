package com.herfy.util.obfuscator

import com.herfy.base.BaseApplication
import com.herfy.util.AppLogger

private val salt: Array<Any> = arrayOf(BaseApplication::class.java, String::class, Int::class)

fun obfuscate(string: String): ByteArray {
    val text = string.toByteArray(Charsets.UTF_8)
    val cipher = salt.contentToString().toByteArray(Charsets.UTF_8)
    val length = cipher.size

    val encrypted = ByteArray(text.size)

    for ((index, value) in text.withIndex()) {
        encrypted[index] = (value.toInt() xor cipher[index % length].toInt()).toByte()
    }

    AppLogger.info("##### Obfuscator ##### -> String to obfuscate: \"$string\"")
    AppLogger.info("##### Obfuscator ##### -> Salt used: $salt")
    AppLogger.info("##### Obfuscator ##### -> Obfuscated string: val key: ByteArray = ${encrypted.contentToString()}\n")

    return encrypted
}

fun reveal(key: ByteArray): String {
    val cipher = salt.contentToString().toByteArray(Charsets.UTF_8)
    val length = cipher.size

    val decrypted = ByteArray(key.size)

    for ((index, value) in key.withIndex()) {
        decrypted[index] = (value.toInt() xor cipher[index % length].toInt()).toByte()
    }

    return String(decrypted, Charsets.UTF_8)
}