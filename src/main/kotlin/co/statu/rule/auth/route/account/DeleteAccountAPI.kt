package co.statu.rule.auth.route.account

import co.statu.parsek.annotation.Endpoint
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.model.*
import co.statu.rule.auth.AuthConfig
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.db.dao.UserDao
import co.statu.rule.auth.db.impl.UserDaoImpl
import co.statu.rule.auth.error.*
import co.statu.rule.auth.mail.ConfirmDeleteAccountMail
import co.statu.rule.auth.mail.DeletedAccountMail
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.auth.token.ConfirmDeleteAccountToken
import co.statu.rule.auth.util.SecurityUtil
import co.statu.rule.database.DatabaseManager
import co.statu.rule.mail.MailManager
import co.statu.rule.token.db.dao.TokenDao
import co.statu.rule.token.db.impl.TokenDaoImpl
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Parameters.optionalParam
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas.stringSchema
import java.util.*

@Endpoint
class DeleteAccountAPI(
    private val authPlugin: AuthPlugin
) : Api() {
    private val pluginConfigManager by lazy {
        authPlugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<AuthConfig>
    }

    private val databaseManager by lazy {
        authPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    private val mailManager by lazy {
        authPlugin.pluginBeanContext.getBean(MailManager::class.java)
    }

    private val authProvider by lazy {
        authPlugin.pluginBeanContext.getBean(AuthProvider::class.java)
    }

    override val paths = listOf(Path("/account", RouteType.DELETE))

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .queryParameter(optionalParam("code", stringSchema()))
            .queryParameter(optionalParam("email", stringSchema()))
            .queryParameter(optionalParam("recaptcha", stringSchema()))
            .queryParameter(optionalParam("signature", stringSchema()))
            .build()

    private val userDao: UserDao = UserDaoImpl()

    private val tokenDao: TokenDao = TokenDaoImpl()

    private val confirmDeleteAccountToken = ConfirmDeleteAccountToken()

    private val jdbcPool by lazy {
        databaseManager.getConnectionPool()
    }

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)

        val magicCode = parameters.queryParameter("code")?.string?.lowercase()?.replace("-", "")
        val email = parameters.queryParameter("email")?.string
        val recaptcha = parameters.queryParameter("recaptcha")?.string
        val signature = parameters.queryParameter("signature")?.string

        if (magicCode != null && email != null) {
            handleVerifyRequest(magicCode, email, recaptcha, signature)
        } else {
            handleDeleteRequest(context)
        }

        return Successful()
    }

    private suspend fun handleVerifyRequest(
        magicCode: String,
        email: String,
        recaptcha: String?,
        signature: String?
    ) {
        if (magicCode.isBlank()) {
            throw InvalidCode()
        }

        if (email.isBlank()) {
            throw InvalidEmail()
        }

        val config = pluginConfigManager.config
        val secretKey = config.secretKey
        val recaptchaConfig = config.recaptchaConfig

        if (((recaptchaConfig.enabled && recaptcha.isNullOrBlank()) || (!recaptchaConfig.enabled && recaptcha == null)) && signature.isNullOrBlank()) {
            throw InvalidSignatureOrRecaptcha()
        }

        if (!recaptcha.isNullOrBlank()) {
            authProvider.validateRecaptcha(recaptcha)
        }

        if (!signature.isNullOrBlank() && signature != SecurityUtil.encodeSha256HMAC(secretKey, email + magicCode)) {
            throw InvalidSignature()
        }

        val userId = userDao.getUserIdFromEmail(email, jdbcPool) ?: throw InvalidCode()

        tokenDao.getByTokenSubjectAndType(
            magicCode,
            userId.toString(),
            confirmDeleteAccountToken,
            jdbcPool
        ) ?: throw InvalidCode()

        tokenDao.deleteBySubject(userId.toString(), jdbcPool)

        val user = userDao.getById(userId, jdbcPool)!!

        val deletedUserTime = System.currentTimeMillis()

        user.name = "Deleted - $deletedUserTime"
        user.surname = ""
        user.fullName = "Deleted - $deletedUserTime"
        user.email = "deleted-account-${deletedUserTime}@parsek.backend"
        user.active = false

        userDao.update(user, jdbcPool)

        sendDeletedAccountMail(email)
    }

    private suspend fun handleDeleteRequest(context: RoutingContext) {
        if (!authProvider.isLoggedIn(context)) {
            throw NotLoggedIn()
        }

        val userId = authProvider.getUserIdFromRoutingContext(context)

        val email = userDao.getEmailFromUserId(userId, jdbcPool)!!

        sendDeleteAccountLink(userId, email)
    }

    private suspend fun sendDeleteAccountLink(userId: UUID, email: String) {
        mailManager.sendMail(userId, email, ConfirmDeleteAccountMail(pluginConfigManager))
    }

    private suspend fun sendDeletedAccountMail(email: String) {
        mailManager.sendMail(UUID.randomUUID(), email, DeletedAccountMail())
    }
}