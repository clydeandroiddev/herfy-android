package com.herfy.util.hmac

import android.util.Base64
import com.herfy.util.obfuscator.Keychain
import com.herfy.util.obfuscator.reveal
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.EnvUtil
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacUtil {

    @JvmStatic
    fun encrypt(data: String?): String {
        val secret = CacheUtil.getInstance().getSharedStringConfig(getDefaultSharedStringConfig())
        val algorithm = "HmacSHA256"
        val shaHMAC = Mac.getInstance(algorithm)
        val secretKey = SecretKeySpec(secret.toByteArray(), algorithm)
        shaHMAC.init(secretKey)

        var dataToEncrypt = data.orEmpty()
        if(data == null || data.equals("null", ignoreCase = true)) {
            dataToEncrypt = ""
        }

        val bytes = shaHMAC.doFinal(dataToEncrypt.toByteArray())
        val bytesHexString = bytesToHex(bytes)

        val signatureByteArray = bytesHexString.toByteArray(StandardCharsets.UTF_8)
        val signature = Base64.encodeToString(signatureByteArray, Base64.NO_WRAP)

        return signature
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789abcdef".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        var j = 0
        var v: Int
        while (j < bytes.size) {
            v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            j++
        }
        return String(hexChars)
    }

    @JvmStatic
    fun getSharedStringRemoteConfigKey(): String {
        return reveal(
            if(EnvUtil.isDevelop()) {
                Keychain.SHARED_PROJECT_REMOTE_CONFIG_DEVELOP_KEY
            } else if(EnvUtil.isMarketing()) {
                Keychain.SHARED_PROJECT_REMOTE_CONFIG_MARKETING_KEY
            } else if(EnvUtil.isMerchant()) {
                Keychain.SHARED_PROJECT_REMOTE_CONFIG_MERCHANT_KEY
            } else if(EnvUtil.isConsumer()) {
                Keychain.SHARED_PROJECT_REMOTE_CONFIG_CONSUMER_KEY
            } else if(EnvUtil.isKiosk()) {
                Keychain.SHARED_PROJECT_REMOTE_CONFIG_KIOSK_KEY
            } else if(EnvUtil.isStaging()) {
                Keychain.SHARED_PROJECT_REMOTE_CONFIG_STAGING_KEY
            } else {
                Keychain.SHARED_PROJECT_REMOTE_CONFIG_KEY
            }
        )
    }

    private fun getDefaultSharedStringConfig(): String {
        return reveal(
            if(EnvUtil.isDevelop()) {
                Keychain.SHARED_STRING_CONFIG_DEVELOP
            } else if(EnvUtil.isMarketing()) {
                Keychain.SHARED_STRING_CONFIG_MARKETING
            } else if(EnvUtil.isMerchant()) {
                Keychain.SHARED_STRING_CONFIG_MERCHANT
            } else if(EnvUtil.isConsumer()) {
                Keychain.SHARED_STRING_CONFIG_CONSUMER
            } else if(EnvUtil.isKiosk()) {
                Keychain.SHARED_STRING_CONFIG_KIOSK
            } else if(EnvUtil.isStaging()) {
                Keychain.SHARED_STRING_CONFIG_STAGING
            } else {
                Keychain.SHARED_STRING_CONFIG
            }
        )
    }

}