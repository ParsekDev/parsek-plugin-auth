package co.statu.rule.auth.config.migration

import co.statu.parsek.annotation.Migration
import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

@Migration
class ConfigMigration9to10 : PluginConfigMigration(9, 10, "Add login config") {
    override fun migrate(config: JsonObject) {
        val loginConfig = JsonObject()

        loginConfig.put("singleSession", false)

        config.put("loginConfig", loginConfig)
    }
}