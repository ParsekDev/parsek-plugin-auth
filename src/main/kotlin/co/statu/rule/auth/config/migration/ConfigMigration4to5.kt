package co.statu.rule.auth.config.migration

import co.statu.parsek.annotation.Migration
import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

@Migration
class ConfigMigration4to5 : PluginConfigMigration(4, 5, "Add invitationConfig") {
    override fun migrate(config: JsonObject) {
        val invitationConfig = JsonObject()

        invitationConfig.put("enabled", false)
        invitationConfig.put("defaultAmount", 50)

        config.put("invitationConfig", invitationConfig)
    }
}