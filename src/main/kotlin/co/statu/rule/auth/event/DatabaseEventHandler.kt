package co.statu.rule.auth.event

import co.statu.rule.auth.AuthPlugin
import co.statu.rule.database.DatabaseManager
import co.statu.rule.database.event.DatabaseEventListener

class DatabaseEventHandler : DatabaseEventListener {
    override suspend fun onReady(databaseManager: DatabaseManager) {
        databaseManager.migrateNewPluginId("auth", AuthPlugin.INSTANCE.context.pluginId, AuthPlugin.INSTANCE)
        databaseManager.initialize(AuthPlugin.INSTANCE, AuthPlugin.tables, AuthPlugin.migrations)

        AuthPlugin.databaseManager = databaseManager
    }
}