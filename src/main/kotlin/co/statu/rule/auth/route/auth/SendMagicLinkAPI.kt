package co.statu.rule.auth.route.auth

import co.statu.parsek.annotation.Endpoint
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.model.*
import co.statu.rule.auth.AuthConfig
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.InvitationCodeSystem
import co.statu.rule.auth.db.dao.UserDao
import co.statu.rule.auth.db.impl.UserDaoImpl
import co.statu.rule.auth.error.InvalidEmail
import co.statu.rule.auth.error.VerifyCodeNotAvailable
import co.statu.rule.auth.mail.MagicLoginMail
import co.statu.rule.auth.mail.MagicRegisterMail
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.auth.token.MagicLoginToken
import co.statu.rule.auth.token.MagicRegisterToken
import co.statu.rule.auth.util.TimeUtil
import co.statu.rule.database.DatabaseManager
import co.statu.rule.mail.MailManager
import co.statu.rule.token.db.dao.TokenDao
import co.statu.rule.token.db.impl.TokenDaoImpl
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.RequestPredicate
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies.json
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas.objectSchema
import io.vertx.json.schema.common.dsl.Schemas.stringSchema
import java.util.*

@Endpoint
class SendMagicLinkAPI(
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

    private val tokenDao: TokenDao = TokenDaoImpl()

    private val invitationCodeSystem by lazy {
        authPlugin.pluginBeanContext.getBean(InvitationCodeSystem::class.java)
    }

    private val magicLoginToken = MagicLoginToken()

    private val magicRegisterToken = MagicRegisterToken()

    override val paths = listOf(Path("/auth/magic-link", RouteType.POST))

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .body(
                json(
                    objectSchema()
                        .requiredProperty("email", stringSchema())
                        .requiredProperty("recaptcha", stringSchema())
                        .optionalProperty("inviteCode", stringSchema())
                )
            )
            .predicate(RequestPredicate.BODY_REQUIRED)
            .build()

    private val userDao: UserDao = UserDaoImpl()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val data = parameters.body().jsonObject

        val email = data.getString("email")
        val recaptcha = data.getString("recaptcha")
        val inviteCode = data.getString("inviteCode")

        if (email.endsWith("@parsek.backend")) {
            throw InvalidEmail()
        }

        authProvider.validateLoginInput(email, recaptcha)

        val jdbcPool = databaseManager.getConnectionPool()

        val config = pluginConfigManager.config

        val userId = userDao.getUserIdFromEmail(email, jdbcPool)

        if (config.resendCodeTime != null) {
            val token = tokenDao.getLastBySubjectAndType(
                userId?.toString() ?: email,
                if (userId != null) magicLoginToken else magicRegisterToken,
                jdbcPool
            )

            if (token != null && TimeUtil.getDifferenceInSeconds(token.startDate) >= config.resendCodeTime) {
                throw VerifyCodeNotAvailable(
                    extras = mapOf(
                        "resendCodeTime" to TimeUtil.getTimeAfterSeconds(token.startDate, config.resendCodeTime)
                    )
                )
            }
        }

        if (userId != null) { // user registered
            sendLoginLink(userId, email)
        } else {
            authProvider.checkTempMail(email)

            if (inviteCode != null) {
                invitationCodeSystem.validateCode(inviteCode, email)
            }

            sendRegisterLink(email)
        }

        val response = mutableMapOf<String, Any?>()

        if (config.resendCodeTime != null) {
            response["resendCodeTime"] = TimeUtil.getTimeAfterSeconds(System.currentTimeMillis(), config.resendCodeTime)
        }

        return Successful(response)
    }

    private suspend fun sendLoginLink(userId: UUID, email: String) {
        mailManager.sendMail(userId, email, MagicLoginMail(pluginConfigManager))
    }

    private suspend fun sendRegisterLink(email: String) {
        mailManager.sendMail(UUID.randomUUID(), email, MagicRegisterMail(pluginConfigManager))
    }
}