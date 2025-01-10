package co.statu.rule.auth.config.migration

import co.statu.parsek.annotation.Migration
import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

@Migration
class ConfigMigration2to3 : PluginConfigMigration(2, 3, "Add domain option to cookie config") {
    override fun migrate(config: JsonObject) {
        val cookieConfig = config.getJsonObject("cookieConfig")

        cookieConfig.put("domain", null)
    }
}