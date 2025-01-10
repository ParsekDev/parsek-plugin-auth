package co.statu.rule.auth.config.migration

import co.statu.parsek.annotation.Migration
import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

@Migration
class ConfigMigration3to4 : PluginConfigMigration(3, 4, "Add whitelistUrl") {
    override fun migrate(config: JsonObject) {
        config.put("whitelistUrl", null)
    }
}