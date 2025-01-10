package co.statu.rule.auth.config.migration

import co.statu.parsek.annotation.Migration
import co.statu.parsek.api.config.PluginConfigMigration
import io.vertx.core.json.JsonObject

@Migration
class ConfigMigration6to7 : PluginConfigMigration(6, 7, "Add registerFields") {
    override fun migrate(config: JsonObject) {
        config.put("registerFields", listOf<Any>())
    }
}