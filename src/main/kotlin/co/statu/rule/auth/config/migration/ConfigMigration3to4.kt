package co.statu.rule.auth.config.migration

import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

class ConfigMigration3to4(
    override val FROM_VERSION: Int = 3,
    override val VERSION: Int = 4,
    override val VERSION_INFO: String = "Add whitelistUrl"
) : PluginConfigMigration() {
    override fun migrate(config: JsonObject) {
        config.put("whitelistUrl", null)
    }
}