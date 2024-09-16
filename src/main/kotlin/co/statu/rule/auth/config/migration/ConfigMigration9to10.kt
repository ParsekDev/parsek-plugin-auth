package co.statu.rule.auth.config.migration

import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

class ConfigMigration9to10(
    override val FROM_VERSION: Int = 9,
    override val VERSION: Int = 10,
    override val VERSION_INFO: String = "Add login config"
) : PluginConfigMigration() {
    override fun migrate(config: JsonObject) {
        val loginConfig = JsonObject()

        loginConfig.put("singleSession", false)

        config.put("loginConfig", loginConfig)
    }
}