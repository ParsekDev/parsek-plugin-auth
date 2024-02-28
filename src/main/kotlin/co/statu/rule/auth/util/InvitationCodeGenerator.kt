package co.statu.rule.auth.util

object InvitationCodeGenerator {
    private const val codeLength = 15
    private const val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    fun generate(): String {
        return (1..codeLength)
            .map { allowedChars.random() }
            .joinToString("")
    }
}