package co.statu.rule.auth.config.migration

import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

class ConfigMigration5to6(
    override val FROM_VERSION: Int = 5,
    override val VERSION: Int = 6,
    override val VERSION_INFO: String = "Add tempMailCheckConfig"
) : PluginConfigMigration() {
    override fun migrate(config: JsonObject) {
        val tempMailCheckConfig = JsonObject()

        tempMailCheckConfig.put("enabled", true)

        config.put("tempMailCheckConfig", tempMailCheckConfig)
    }
}