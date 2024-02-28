package co.statu.rule.auth.mail

import co.statu.parsek.api.config.PluginConfigManager
import co.statu.rule.auth.AuthConfig
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.db.dao.UserDao
import co.statu.rule.auth.token.ConfirmDeleteAccountToken
import co.statu.rule.auth.util.OneTimeCodeGenerator
import co.statu.rule.auth.util.SecurityUtil
import co.statu.rule.database.Dao
import co.statu.rule.database.DatabaseManager
import co.statu.rule.mail.Mail
import co.statu.rule.token.provider.TokenProvider
import io.vertx.core.json.JsonObject
import io.vertx.jdbcclient.JDBCPool
import java.util.*

class ConfirmDeleteAccountMail(private val pluginConfigManager: PluginConfigManager<AuthConfig>) : Mail {
    override val templatePath = "confirm-delete-account.hbs"
    override val subject = "Hesap Silinme Onayı - ${getBrandName()}"

    private val confirmDeleteAccountToken by lazy {
        ConfirmDeleteAccountToken()
    }

    private val userDao by lazy {
        Dao.get<UserDao>(AuthPlugin.tables)
    }

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

        tokenProvider.invalidateTokensBySubjectAndType(userId.toString(), confirmDeleteAccountToken, jdbcPool)

        tokenProvider.saveToken(
            code.replace("-", ""),
            userId.toString(),
            confirmDeleteAccountToken,
            confirmDeleteAccountToken.getExpireDateFromNow(),
            jdbcPool
        )

        val signature = SecurityUtil.encodeSha256HMAC(secretKey, email + formattedCode)

        val user = userDao.getById(userId, jdbcPool)!!

        parameters.put("link", "$uiAddress/account/delete?email=$email&code=$code&signature=$signature")
        parameters.put("code", code)
        parameters.put("name", " " + user.name)

        return parameters
    }
}