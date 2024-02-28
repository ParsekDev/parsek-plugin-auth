package co.statu.rule.auth.config.migration

import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

class ConfigMigration2to3(
    override val FROM_VERSION: Int = 2,
    override val VERSION: Int = 3,
    override val VERSION_INFO: String = "Add domain option to cookie config"
) : PluginConfigMigration() {
    override fun migrate(config: JsonObject) {
        val cookieConfig = config.getJsonObject("cookieConfig")

        cookieConfig.put("domain", null)
    }
}