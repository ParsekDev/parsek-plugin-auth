package co.statu.rule.auth.util

import java.util.regex.Pattern

object StringUtil {
    fun anonymizeEmail(email: String): String {
        val (username, domain) = email.split('@')
        val convertedUsername = "x".repeat(username.length)
        return "$convertedUsername@$domain"
    }

    fun extractOriginalEmailFromAlias(emailWithAlias: String): String {
        // Define a regex pattern to extract the original email
        val pattern = Pattern.compile("(.+)\\+(.+)@(.+)")
        val matcher = pattern.matcher(emailWithAlias)

        // Check if the pattern matches the input string
        return if (matcher.matches()) {
            // Extract the original email by combining the first and third groups
            val originalEmail = "${matcher.group(1)}@${matcher.group(3)}"
            originalEmail
        } else {
            // If the pattern doesn't match, return the original string
            emailWithAlias
        }
    }

    fun extractOriginalEmail(email: String): String {
        // Extract the original email from an alias
        val aliasRemoved = extractOriginalEmailFromAlias(email)

        // Remove dots from the username part
        val usernameWithoutDots = aliasRemoved.substringBefore('@').replace(".", "")

        // Combine the modified username with the domain
        return "$usernameWithoutDots@${aliasRemoved.substringAfter('@')}"
    }
}