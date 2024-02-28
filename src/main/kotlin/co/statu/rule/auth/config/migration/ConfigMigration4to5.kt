package co.statu.rule.auth.config.migration

import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

class ConfigMigration4to5(
    override val FROM_VERSION: Int = 4,
    override val VERSION: Int = 5,
    override val VERSION_INFO: String = "Add invitationConfig"
) : PluginConfigMigration() {
    override fun migrate(config: JsonObject) {
        val invitationConfig = JsonObject()

        invitationConfig.put("enabled", false)
        invitationConfig.put("defaultAmount", 50)

        config.put("invitationConfig", invitationConfig)
    }
}