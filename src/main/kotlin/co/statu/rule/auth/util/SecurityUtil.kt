package co.statu.rule.auth.util

import org.apache.commons.codec.binary.Hex
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SecurityUtil {
    @Throws(java.lang.Exception::class)
    fun encodeSha256HMAC(key: String, data: String): String? {
        val sha256HMAC = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(charset("UTF-8")), "HmacSHA256")
        sha256HMAC.init(secretKey)
        return Hex.encodeHexString(sha256HMAC.doFinal(data.toByteArray(charset("UTF-8"))))
    }
}