package co.statu.rule.auth.route.profile

import co.statu.parsek.PluginEventManager
import co.statu.parsek.model.Path
import co.statu.parsek.model.Result
import co.statu.parsek.model.RouteType
import co.statu.parsek.model.Successful
import co.statu.rule.auth.api.LoggedInApi
import co.statu.rule.auth.event.AuthEventListener
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.database.DatabaseManager
import io.vertx.ext.web.RoutingContext
import io.vertx.json.schema.SchemaParser

class GetProfileAPI(
    private val authProvider: AuthProvider,
    private val databaseManager: DatabaseManager,
    private val pluginEventManager: PluginEventManager
) : LoggedInApi() {
    override val paths = listOf(Path("/profile", RouteType.GET))

    override fun getValidationHandler(schemaParser: SchemaParser) = null

    override suspend fun handle(context: RoutingContext): Result {
        val userId = authProvider.getUserIdFromRoutingContext(context)

        val jdbcPool = databaseManager.getConnectionPool()

        val user = userDao.getById(userId, jdbcPool)!!

        val response = mutableMapOf<String, Any?>()

        response["name"] = user.name
        response["surname"] = user.surname
        response["email"] = user.email
        response["lang"] = user.lang

        val authEventHandlers = pluginEventManager.getEventHandlers<AuthEventListener>()

        authEventHandlers.forEach { it.onGetProfile(user, response) }

        return Successful(response)
    }
}