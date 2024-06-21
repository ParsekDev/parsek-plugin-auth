package co.statu.rule.auth.route.auth

import co.statu.parsek.annotation.Endpoint
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.model.*
import co.statu.rule.auth.AuthConfig
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.db.dao.UserDao
import co.statu.rule.auth.db.impl.UserDaoImpl
import co.statu.rule.auth.error.InvalidCode
import co.statu.rule.auth.error.InvalidEmail
import co.statu.rule.auth.error.InvalidSignature
import co.statu.rule.auth.error.InvalidSignatureOrRecaptcha
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.auth.token.MagicChangeEmailToken
import co.statu.rule.auth.token.MagicLoginToken
import co.statu.rule.auth.token.MagicRegisterToken
import co.statu.rule.auth.token.RegisterToken
import co.statu.rule.auth.util.SecurityUtil
import co.statu.rule.database.DatabaseManager
import co.statu.rule.token.db.dao.TokenDao
import co.statu.rule.token.db.impl.TokenDaoImpl
import co.statu.rule.token.provider.TokenProvider
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
class VerifyMagicLinkAPI(
    private val authPlugin: AuthPlugin
) : Api() {
    private val pluginConfigManager by lazy {
        authPlugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<AuthConfig>
    }

    private val databaseManager by lazy {
        authPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    private val tokenProvider by lazy {
        authPlugin.pluginBeanContext.getBean(TokenProvider::class.java)
    }

    private val authProvider by lazy {
        authPlugin.pluginBeanContext.getBean(AuthProvider::class.java)
    }

    override val paths = listOf(Path("/auth/magic-link/verify", RouteType.POST))

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .body(
                json(
                    objectSchema()
                        .requiredProperty("code", stringSchema())
                        .requiredProperty("email", stringSchema())
                        .optionalProperty("recaptcha", stringSchema())
                        .optionalProperty("signature", stringSchema())
                )
            )
            .predicate(RequestPredicate.BODY_REQUIRED)
            .build()

    private val tokenDao: TokenDao = TokenDaoImpl()

    private val userDao: UserDao = UserDaoImpl()

    private val magicRegisterToken = MagicRegisterToken()

    private val magicChangeEmailToken = MagicChangeEmailToken()

    private val registerTokenObject = RegisterToken()

    private val magicLoginToken = MagicLoginToken()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val data = parameters.body().jsonObject

        val magicCode = data.getString("code").lowercase().replace("-", "")
        val email = data.getString("email")
        val recaptcha = data.getString("recaptcha")
        val signature = data.getString("signature")

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

        val jdbcPool = databaseManager.getConnectionPool()

        val changeEmailToken = tokenDao.getByTokenSubjectAndType(magicCode, email, magicChangeEmailToken, jdbcPool)

        if (changeEmailToken != null) {
            val userId = UUID.fromString(changeEmailToken.additionalClaims.getString("userId"))
            val user = userDao.getById(userId, jdbcPool)!!

            tokenProvider.invalidateTokensBySubjectAndType(user.email, magicLoginToken, jdbcPool)
            tokenProvider.invalidateTokensBySubjectAndType(email, magicChangeEmailToken, jdbcPool)

            user.email = email

            userDao.update(user, jdbcPool)

            return Successful(
                mapOf(
                    "magicLinkType" to MagicLinkType.CHANGE_EMAIl
                )
            )
        }

        val registerToken =
            tokenDao.getByTokenSubjectAndType(magicCode, email, magicRegisterToken, jdbcPool)

        if (registerToken != null) {
            tokenProvider.invalidateTokensBySubjectAndType(email, magicRegisterToken, jdbcPool)

            if (registerToken.expireDate < System.currentTimeMillis()) {
                throw InvalidCode()
            }

            val (token, expireDate) = tokenProvider.generateToken(
                email,
                registerTokenObject,
                mapOf("email" to email)
            )

            tokenProvider.saveToken(token, email, registerTokenObject, expireDate, jdbcPool = jdbcPool)

            return Successful(
                mapOf(
                    "email" to email,
                    "jwtToken" to token,
                    "magicLinkType" to MagicLinkType.REGISTER
                )
            )
        }

        val userId = userDao.getUserIdFromEmail(email, jdbcPool) ?: throw InvalidCode()

        val loginToken = tokenDao.getByTokenSubjectAndType(
            magicCode,
            userId.toString(),
            magicLoginToken,
            jdbcPool
        ) ?: throw InvalidCode()

        if (loginToken.expireDate < System.currentTimeMillis()) {
            throw InvalidCode()
        }

        tokenProvider.invalidateByTokenId(loginToken.id, jdbcPool)

        authProvider.authenticate(email)

        val (authToken, csrfToken) = authProvider.login(email, jdbcPool)

        userDao.updateLastLoginDate(userId, jdbcPool)

        val cookieSet = authProvider.setCookies(context, authToken, csrfToken)

        val response = mutableMapOf<String, Any>(
            "magicLinkType" to MagicLinkType.LOGIN
        )

        if (cookieSet) {
            response["csrfToken"] = csrfToken
        }

        response["jwtToken"] = authToken

        return Successful(response)
    }

    enum class MagicLinkType {
        REGISTER, LOGIN, CHANGE_EMAIl
    }
}