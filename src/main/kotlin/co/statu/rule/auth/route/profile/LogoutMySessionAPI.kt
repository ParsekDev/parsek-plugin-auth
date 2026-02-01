package co.statu.rule.auth.route.profile

import co.statu.parsek.annotation.Endpoint
import co.statu.parsek.model.Path
import co.statu.parsek.model.Result
import co.statu.parsek.model.RouteType
import co.statu.parsek.model.Successful
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.api.LoggedInApi
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.auth.token.AuthenticationToken
import co.statu.rule.database.DatabaseManager
import co.statu.rule.token.provider.TokenProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.RequestPredicate
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies.json
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.objectSchema
import io.vertx.json.schema.common.dsl.Schemas.stringSchema
import java.util.*

@Endpoint
class LogoutMySessionAPI(
    private val authPlugin: AuthPlugin
) : LoggedInApi() {

    private val databaseManager by lazy {
        authPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    private val authProvider by lazy {
        authPlugin.pluginBeanContext.getBean(AuthProvider::class.java)
    }

    private val tokenProvider by lazy {
        authPlugin.pluginBeanContext.getBean(TokenProvider::class.java)
    }

    override val paths = listOf(Path("/profile/sessions/logout", RouteType.POST))

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .body(
                json(
                    objectSchema()
                        .requiredProperty("id", stringSchema())
                )
            )
            .predicate(RequestPredicate.BODY_REQUIRED)
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val data = parameters.body().jsonObject
        val sessionId = UUID.fromString(data.getString("id"))
        
        val jdbcPool = databaseManager.getConnectionPool()

        tokenProvider.invalidateByTokenId(sessionId, jdbcPool)

        return Successful()
    }
}
