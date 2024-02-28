package co.statu.rule.auth.route.auth

import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.model.*
import co.statu.rule.auth.AuthConfig
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.db.dao.UserDao
import co.statu.rule.auth.error.InvalidToken
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.auth.token.RegisterToken
import co.statu.rule.database.Dao.Companion.get
import co.statu.rule.database.DatabaseManager
import co.statu.rule.token.provider.TokenProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.RequestPredicate
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies.json
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas.objectSchema
import io.vertx.json.schema.common.dsl.Schemas.stringSchema

class RegisterAPI(
    private val authProvider: AuthProvider,
    private val tokenProvider: TokenProvider,
    private val databaseManager: DatabaseManager,
    private val pluginConfigManager: PluginConfigManager<AuthConfig>
) : Api() {
    override val paths = listOf(Path("/auth/register", RouteType.POST))

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .body(
                json(
                    objectSchema()
                        .requiredProperty("name", stringSchema())
                        .requiredProperty("surname", stringSchema())
                        .requiredProperty("lang", stringSchema())
                        .requiredProperty("token", stringSchema())
                        .requiredProperty("recaptcha", stringSchema())
                )
            )
            .predicate(RequestPredicate.BODY_REQUIRED)
            .build()

    private val registerTokenObject by lazy {
        RegisterToken()
    }

    private val userDao by lazy {
        get<UserDao>(AuthPlugin.tables)
    }

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val data = parameters.body().jsonObject

        val name = data.getString("name")
        val surname = data.getString("surname")
        val lang = data.getString("lang")
        val registerToken = data.getString("token")
        val recaptcha = data.getString("recaptcha")

        authProvider.validateRegisterInput(
            name,
            surname,
            lang
        )

        authProvider.validateRecaptcha(recaptcha)

        val remoteIP = context.request().remoteAddress().host()

        val jdbcPool = databaseManager.getConnectionPool()

        val isTokenValid = tokenProvider.isTokenValid(registerToken, registerTokenObject)

        if (!isTokenValid) {
            throw InvalidToken()
        }

        val decodedToken = tokenProvider.parseToken(registerToken)

        val claims = decodedToken.claims

        tokenProvider.invalidateToken(registerToken)

        val email = claims["email"]!!.asString()

        tokenProvider.invalidateTokensBySubjectAndType(email, registerTokenObject, jdbcPool)

        val userId = authProvider.register(
            name,
            surname,
            email,
            lang,
            remoteIP = remoteIP,
            jdbcPool = jdbcPool
        )

        authProvider.authenticate(email)

        val (authToken, csrfToken) = authProvider.login(email, jdbcPool)

        userDao.updateLastLoginDate(userId, jdbcPool)

        val cookieSet = authProvider.setCookies(context, authToken, csrfToken)

        val response = mutableMapOf<String, Any>()

        if (cookieSet) {
            response["csrfToken"] = csrfToken
        }

        response["jwtToken"] = authToken

        return Successful(response)
    }
}