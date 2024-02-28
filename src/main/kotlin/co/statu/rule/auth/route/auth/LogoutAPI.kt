package co.statu.rule.auth.route.auth

import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.model.*
import co.statu.rule.auth.AuthConfig
import co.statu.rule.auth.provider.AuthProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.json.schema.SchemaParser

class LogoutAPI(
    private val authProvider: AuthProvider,
    private val pluginConfigManager: PluginConfigManager<AuthConfig>
) : Api() {
    override val paths = listOf(Path("/auth/logout", RouteType.POST))

    override fun getValidationHandler(schemaParser: SchemaParser) = null

    override suspend fun handle(context: RoutingContext): Result {
        authProvider.logout(context)

        val response = context.response()

        val config = pluginConfigManager.config
        val cookieConfig = config.cookieConfig

        if (cookieConfig.enabled) {
            response.putHeader(
                "Set-Cookie",
                listOf(
                    "${cookieConfig.prefix + cookieConfig.csrfTokenName}=deleted;${cookieConfig.domain?.let { " domain=$it;" }} path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT",
                    "${cookieConfig.prefix + cookieConfig.authTokenName}=deleted;${cookieConfig.domain?.let { " domain=$it;" }} path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT"
                )
            )
        }

        return Successful()
    }
}