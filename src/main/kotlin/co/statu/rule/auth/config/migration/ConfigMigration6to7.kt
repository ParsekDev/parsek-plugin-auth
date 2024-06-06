package co.statu.rule.auth.config.migration

import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

class ConfigMigration6to7(
    override val FROM_VERSION: Int = 6,
    override val VERSION: Int = 7,
    override val VERSION_INFO: String = "Add registerFields"
) : PluginConfigMigration() {
    override fun migrate(config: JsonObject) {
        config.put("registerFields", listOf<Any>())
    }
}