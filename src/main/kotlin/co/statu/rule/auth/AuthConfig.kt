package co.statu.rule.auth

import co.statu.parsek.api.config.PluginConfig
import co.statu.parsek.util.KeyGeneratorUtil

class AuthConfig(
    val secretKey: String = KeyGeneratorUtil.generateSecretKey(),
    val recaptchaConfig: RecaptchaConfig = RecaptchaConfig(),
    val cookieConfig: CookieConfig = CookieConfig(),
    val whitelistUrl: String? = null,
    val invitationConfig: InvitationConfig = InvitationConfig(),
    val tempMailCheckConfig: TempMailCheckConfig = TempMailCheckConfig(),
    val registerFields: List<RegisterField> = listOf(),
    val resendCodeTime: Long? = 0
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
            val domain: String? = null,
            val secure: Boolean = true
        )

        data class InvitationConfig(
            val enabled: Boolean = false,
            val defaultAmount: Long = 50
        )

        data class TempMailCheckConfig(
            val enabled: Boolean = true
        )

        data class RegisterField(
            val field: String,
            val isBlankCheck: Boolean = true,
            val optional: Boolean? = false,
            val min: Any? = null,
            val max: Any? = null,
            val regex: String? = null,
            val unique: Boolean = false,
            val upperCaseFirstChar: Boolean? = false,
            val hiddenToUI: Boolean? = false,
            val type: Type,
            val onlyRegister: Boolean = false
        ) {
            companion object {
                enum class Type {
                    STRING,
                    BOOLEAN,
                    INT,
                    FLOAT,
                    DOUBLE
                }
            }
        }
    }
}