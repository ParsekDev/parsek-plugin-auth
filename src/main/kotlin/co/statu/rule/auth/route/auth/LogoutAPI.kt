package co.statu.rule.auth.route.auth

import co.statu.parsek.annotation.Endpoint
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.model.*
import co.statu.rule.auth.AuthConfig
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.provider.AuthProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.json.schema.SchemaParser

@Endpoint
class LogoutAPI(
    private val authPlugin: AuthPlugin
) : Api() {
    private val pluginConfigManager by lazy {
        authPlugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<AuthConfig>
    }

    private val authProvider by lazy {
        authPlugin.pluginBeanContext.getBean(AuthProvider::class.java)
    }

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