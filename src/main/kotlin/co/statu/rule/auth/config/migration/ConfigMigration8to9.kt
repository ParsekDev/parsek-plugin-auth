package co.statu.rule.auth.config.migration

import co.statu.parsek.annotation.Migration
import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

@Migration
class ConfigMigration8to9 : PluginConfigMigration(8, 9, "Add resendCodeTime option") {
    override fun migrate(config: JsonObject) {
        config.put("resendCodeTime", 30L)
    }
}