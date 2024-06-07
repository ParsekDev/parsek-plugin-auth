package co.statu.rule.auth.config.migration

import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

class ConfigMigration7to8(
    override val FROM_VERSION: Int = 7,
    override val VERSION: Int = 8,
    override val VERSION_INFO: String = "Add secure flag to cookie config"
) : PluginConfigMigration() {
    override fun migrate(config: JsonObject) {
        val cookieConfig = config.getJsonObject("cookieConfig")

        cookieConfig.put("secure", true)
    }
}