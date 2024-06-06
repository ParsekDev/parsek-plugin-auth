package co.statu.rule.auth.route.auth

import co.statu.parsek.annotation.Endpoint
import co.statu.parsek.model.*
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.db.dao.UserDao
import co.statu.rule.auth.db.impl.UserDaoImpl
import co.statu.rule.auth.error.InvalidToken
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.auth.token.RegisterToken
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
import kotlin.collections.set

@Endpoint
class RegisterAPI(
    private val authPlugin: AuthPlugin,
) : Api() {
    private val databaseManager by lazy {
        authPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    private val tokenProvider by lazy {
        authPlugin.pluginBeanContext.getBean(TokenProvider::class.java)
    }

    private val authProvider by lazy {
        authPlugin.pluginBeanContext.getBean(AuthProvider::class.java)
    }

    override val paths = listOf(Path("/auth/register", RouteType.POST))

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .body(
                json(
                    objectSchema()
                        .requiredProperty("token", stringSchema())
                        .requiredProperty("recaptcha", stringSchema())
                )
            )
            .predicate(RequestPredicate.BODY_REQUIRED)
            .build()

    private val registerTokenObject = RegisterToken()

    private val userDao: UserDao = UserDaoImpl()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val data = parameters.body().jsonObject

        val registerToken = data.getString("token")
        val recaptcha = data.getString("recaptcha")

        authProvider.validateRegisterInput(data)

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
            email,
            data,
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