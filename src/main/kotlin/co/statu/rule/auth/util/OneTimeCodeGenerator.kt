package co.statu.rule.auth.util

import java.util.*

object OneTimeCodeGenerator {
    private val random = Random()
    private val codeChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()

    fun generate(): String {
        val code = CharArray(10)

        repeat(5) { i ->
            code[i] = codeChars[random.nextInt(codeChars.size)]
        }

        repeat(5) { i ->
            code[i + 5] = codeChars[random.nextInt(codeChars.size)]
        }

        return "${String(code, 0, 5)}-${String(code, 5, 5)}".lowercase()
    }
}