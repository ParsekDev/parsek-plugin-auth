package co.statu.rule.auth.route.auth

import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.model.*
import co.statu.rule.auth.AuthConfig
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.db.dao.UserDao
import co.statu.rule.auth.error.InvalidCode
import co.statu.rule.auth.error.InvalidEmail
import co.statu.rule.auth.error.InvalidSignature
import co.statu.rule.auth.error.InvalidSignatureOrRecaptcha
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.auth.token.MagicLoginToken
import co.statu.rule.auth.token.MagicRegisterToken
import co.statu.rule.auth.token.RegisterToken
import co.statu.rule.auth.util.SecurityUtil
import co.statu.rule.database.Dao
import co.statu.rule.database.DatabaseManager
import co.statu.rule.token.db.dao.TokenDao
import co.statu.rule.token.provider.TokenProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.RequestPredicate
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies.json
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas.objectSchema
import io.vertx.json.schema.common.dsl.Schemas.stringSchema

class VerifyMagicLinkAPI(
    private val authProvider: AuthProvider,
    private val tokenProvider: TokenProvider,
    private val pluginConfigManager: PluginConfigManager<AuthConfig>,
    private val databaseManager: DatabaseManager
) : Api() {
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

    private val tokenDao by lazy {
        Dao.get<TokenDao>(AuthPlugin.externalTables)
    }

    private val userDao by lazy {
        Dao.get<UserDao>(AuthPlugin.tables)
    }

    private val magicRegisterToken by lazy {
        MagicRegisterToken()
    }

    private val registerTokenObject by lazy {
        RegisterToken()
    }

    private val magicLoginToken by lazy {
        MagicLoginToken()
    }

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

        val registerToken =
            tokenDao.getByTokenSubjectAndType(magicCode, email, magicRegisterToken, jdbcPool)

        if (registerToken != null) {
            tokenProvider.invalidateToken(magicCode)

            if (registerToken.expireDate < System.currentTimeMillis()) {
                throw InvalidCode()
            }

            val (token, expireDate) = tokenProvider.generateToken(
                email,
                registerTokenObject,
                mapOf("email" to email)
            )

            tokenProvider.saveToken(token, email, registerTokenObject, expireDate, jdbcPool)

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

        tokenProvider.invalidateToken(magicCode)

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
        REGISTER, LOGIN
    }
}