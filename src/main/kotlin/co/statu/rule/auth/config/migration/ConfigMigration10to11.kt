package co.statu.rule.auth.config.migration

import co.statu.parsek.annotation.Migration
import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

@Migration
class ConfigMigration10to11 : PluginConfigMigration(10, 11, "Add max sessions to login config") {
    override fun migrate(config: JsonObject) {
        val loginConfig = config.getJsonObject("loginConfig") ?: JsonObject().also { config.put("loginConfig", it) }

        if (!loginConfig.containsKey("maxSessions")) {
            loginConfig.put("maxSessions", 5)
        }
    }
}
