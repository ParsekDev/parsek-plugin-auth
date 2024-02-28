package co.statu.rule.auth

import co.statu.parsek.api.config.PluginConfig
import co.statu.parsek.util.KeyGeneratorUtil

class AuthConfig(
    val secretKey: String = KeyGeneratorUtil.generateSecretKey(),
    val recaptchaConfig: RecaptchaConfig = RecaptchaConfig(),
    val cookieConfig: CookieConfig = CookieConfig(),
    val whitelistUrl: String? = null,
    val invitationConfig: InvitationConfig = InvitationConfig(),
    val tempMailCheckConfig: TempMailCheckConfig = TempMailCheckConfig()
) : PluginConfig() {
    companion object {
        data class RecaptchaConfig(
            val enabled: Boolean = false,
            val secret: String = ""
        )

        data class CookieConfig(
            val enabled: Boolean = true,
            val prefix: String = "parsek_",
            val authTokenName: String = "auth_token",
            val csrfTokenName: String = "csrf_token",
            val csrfHeader: String = "X-CSRF-Token",
            val domain: String? = null
        )

        data class InvitationConfig(
            val enabled: Boolean = false,
            val defaultAmount: Long = 50
        )

        data class TempMailCheckConfig(
            val enabled: Boolean = true
        )
    }
}