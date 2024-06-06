package co.statu.rule.auth.event

import co.statu.parsek.api.annotation.EventListener
import co.statu.rule.auth.AuthFieldManager
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.database.DatabaseManager
import co.statu.rule.database.event.DatabaseEventListener

@EventListener
class DatabaseEventHandler(private val authPlugin: AuthPlugin) : DatabaseEventListener {
    private val authFieldManager by lazy {
        authPlugin.pluginBeanContext.getBean(AuthFieldManager::class.java)
    }

    override suspend fun onReady(databaseManager: DatabaseManager) {
        databaseManager.migrateNewPluginId("auth", authPlugin.pluginId, authPlugin)
        databaseManager.initialize(authPlugin, authPlugin)

        AuthPlugin.databaseManager = databaseManager

        authFieldManager.init()
    }
}