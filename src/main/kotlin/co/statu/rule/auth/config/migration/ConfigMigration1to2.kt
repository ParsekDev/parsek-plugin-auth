package co.statu.rule.auth.config.migration

import co.statu.parsek.annotation.Migration
import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

@Migration
class ConfigMigration1to2 : PluginConfigMigration(1, 2, "Add cookie config") {
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