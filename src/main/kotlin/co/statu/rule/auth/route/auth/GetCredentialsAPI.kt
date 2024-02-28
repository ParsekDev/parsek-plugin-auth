package co.statu.rule.auth.route.auth

import co.statu.parsek.model.Path
import co.statu.parsek.model.Result
import co.statu.parsek.model.RouteType
import co.statu.parsek.model.Successful
import co.statu.rule.auth.api.LoggedInApi
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.database.DatabaseManager
import io.vertx.ext.web.RoutingContext
import io.vertx.json.schema.SchemaParser

class GetCredentialsAPI(
    private val authProvider: AuthProvider,
    private val databaseManager: DatabaseManager
) : LoggedInApi() {
    override val paths = listOf(Path("/auth/credentials", RouteType.GET))

    override fun getValidationHandler(schemaParser: SchemaParser) = null

    override suspend fun handle(context: RoutingContext): Result {
        val userId = authProvider.getUserIdFromRoutingContext(context)

        val jdbcPool = databaseManager.getConnectionPool()

        val user = userDao.getById(userId, jdbcPool)!!

        val panelAccess = user.permissionGroupId != null

        val response = mutableMapOf(
            "id" to user.id,
            "name" to user.name,
            "surname" to user.surname,
            "fullName" to user.fullName,
            "email" to user.email,
            "lang" to user.lang,
            "registerDate" to user.registerDate,
            "lastLoginDate" to user.lastLoginDate,
            "lastActivityTime" to user.lastActivityTime
        )

        if (panelAccess) {
            response["lastPanelActivityTime"] = user.lastPanelActivityTime
            response["panelAccess"] = true
        }

        return Successful(response)
    }
}