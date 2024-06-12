package co.statu.rule.auth.config.migration

import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

class ConfigMigration8to9(
    override val FROM_VERSION: Int = 8,
    override val VERSION: Int = 9,
    override val VERSION_INFO: String = "Add resendCodeTime option"
) : PluginConfigMigration() {
    override fun migrate(config: JsonObject) {
        config.put("resendCodeTime", 30L)
    }
}