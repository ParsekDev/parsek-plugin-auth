package co.statu.rule.auth.event

import co.statu.parsek.api.annotation.EventListener
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.database.DatabaseManager
import co.statu.rule.database.event.DatabaseEventListener

@EventListener
class DatabaseEventHandler(private val authPlugin: AuthPlugin) : DatabaseEventListener {
    override suspend fun onReady(databaseManager: DatabaseManager) {
        databaseManager.initialize(authPlugin, authPlugin)

        AuthPlugin.databaseManager = databaseManager
    }
}