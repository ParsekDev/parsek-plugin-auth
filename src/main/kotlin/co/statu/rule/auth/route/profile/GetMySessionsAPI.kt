package co.statu.rule.auth.route.profile

import co.statu.parsek.annotation.Endpoint
import co.statu.parsek.model.Path
import co.statu.parsek.model.Result
import co.statu.parsek.model.RouteType
import co.statu.parsek.model.Successful
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.rule.auth.AuthConfig
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.api.LoggedInApi
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.auth.token.AuthenticationToken
import co.statu.rule.database.DatabaseManager
import co.statu.rule.token.provider.TokenProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.json.schema.SchemaRepository

@Endpoint
class GetMySessionsAPI(
    private val authPlugin: AuthPlugin
) : LoggedInApi() {

    private val pluginConfigManager by lazy {
        authPlugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<AuthConfig>
    }

    private val databaseManager by lazy {
        authPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    private val authProvider by lazy {
        authPlugin.pluginBeanContext.getBean(AuthProvider::class.java)
    }

    private val tokenProvider by lazy {
        authPlugin.pluginBeanContext.getBean(TokenProvider::class.java)
    }

    override val paths = listOf(Path("/profile/sessions", RouteType.GET))

    override fun getValidationHandler(schemaRepository: SchemaRepository) = null

    private val authenticationToken = AuthenticationToken()

    override suspend fun handle(context: RoutingContext): Result {
        val userId = authProvider.getUserIdFromRoutingContext(context)
        val currentToken = authProvider.getTokenFromRoutingContext(context)
        val jdbcPool = databaseManager.getConnectionPool()

        val tokens = tokenProvider.getAllBySubjectAndType(userId.toString(), authenticationToken, jdbcPool)

        val sessions = tokens.map {
            mapOf(
                "id" to it.id.toString(),
                "ip" to (it.additionalClaims.getString("ip") ?: "-"),
                "userAgent" to (it.additionalClaims.getString("userAgent") ?: "-"),
                "lastActivityTime" to it.startDate,
                "expireDate" to it.expireDate,
                "isCurrent" to (it.token == currentToken)
            )
        }

        return Successful(
            mapOf(
                "sessions" to sessions,
                "maxSessions" to pluginConfigManager.config.loginConfig.maxSessions
            )
        )
    }
}
