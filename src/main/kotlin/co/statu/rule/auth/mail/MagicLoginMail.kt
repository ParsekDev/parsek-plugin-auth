package co.statu.rule.auth.mail

import co.statu.parsek.api.config.PluginConfigManager
import co.statu.rule.auth.AuthConfig
import co.statu.rule.auth.db.dao.UserDao
import co.statu.rule.auth.db.impl.UserDaoImpl
import co.statu.rule.auth.token.MagicLoginToken
import co.statu.rule.auth.util.OneTimeCodeGenerator
import co.statu.rule.auth.util.SecurityUtil
import co.statu.rule.database.DatabaseManager
import co.statu.rule.mail.Mail
import co.statu.rule.token.provider.TokenProvider
import io.vertx.core.json.JsonObject
import io.vertx.jdbcclient.JDBCPool
import java.util.*

class MagicLoginMail(private val pluginConfigManager: PluginConfigManager<AuthConfig>) : Mail {
    override val templatePath = "magic-link.hbs"
    override val subject = "Hesabına Giriş Yap - ${getBrandName()}"

    private val magicLoginToken = MagicLoginToken()

    private val userDao: UserDao = UserDaoImpl()

    override suspend fun parameterGenerator(
        email: String,
        userId: UUID,
        uiAddress: String,
        databaseManager: DatabaseManager,
        jdbcPool: JDBCPool,
        tokenProvider: TokenProvider
    ): JsonObject {
        val parameters = JsonObject()

        val code = OneTimeCodeGenerator.generate()
        val formattedCode = code.replace("-", "")

        val config = pluginConfigManager.config
        val secretKey = config.secretKey

        tokenProvider.saveToken(
            formattedCode,
            userId.toString(),
            magicLoginToken,
            magicLoginToken.getExpireDateFromNow(),
            jdbcPool = jdbcPool
        )

        val signature = SecurityUtil.encodeSha256HMAC(secretKey, email + formattedCode)

        val user = userDao.getById(userId, jdbcPool)!!

        parameters.put("link", "$uiAddress/auth/link?code=$code&email=$email&signature=$signature")
        parameters.put("code", code)

        user.additionalFields.forEach {
            parameters.put(it.key, it.value)
        }

        return parameters
    }
}
