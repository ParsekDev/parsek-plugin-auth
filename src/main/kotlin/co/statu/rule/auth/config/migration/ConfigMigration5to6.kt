package co.statu.rule.auth.config.migration

import co.statu.parsek.annotation.Migration
import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

@Migration
class ConfigMigration5to6 : PluginConfigMigration(5, 6, "Add tempMailCheckConfig") {
    override fun migrate(config: JsonObject) {
        val tempMailCheckConfig = JsonObject()

        tempMailCheckConfig.put("enabled", true)

        config.put("tempMailCheckConfig", tempMailCheckConfig)
    }
}