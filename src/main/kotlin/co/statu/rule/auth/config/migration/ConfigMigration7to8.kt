package co.statu.rule.auth.config.migration

import co.statu.parsek.annotation.Migration
import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

@Migration
class ConfigMigration7to8 : PluginConfigMigration(7, 8, "Add secure flag to cookie config") {
    override fun migrate(config: JsonObject) {
        val cookieConfig = config.getJsonObject("cookieConfig")

        cookieConfig.put("secure", true)
    }
}