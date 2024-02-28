package co.statu.rule.auth.util

import java.math.BigInteger
import java.security.SecureRandom

object CsrfTokenGenerator {
    fun nextToken(): String {
        val random = SecureRandom()

        return BigInteger(130, random).toString(32)
    }
}
