package co.statu.rule.auth.config.migration

import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

class ConfigMigration1to2(
    override val FROM_VERSION: Int = 1,
    override val VERSION: Int = 2,
    override val VERSION_INFO: String = "Add cookie config"
) : PluginConfigMigration() {
    override fun migrate(config: JsonObject) {
        config.put(
            "cookieConfig", mapOf(
                "enabled" to true,
                "prefix" to "parsek_",
                "authTokenName" to "auth_token",
                "csrfTokenName" to "csrfToken",
                "csrfHeader" to "X-CSRF-Token"
            )
        )
    }
}