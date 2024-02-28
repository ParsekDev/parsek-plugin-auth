package co.statu.rule.auth.api

import co.statu.parsek.error.NoPermission
import co.statu.rule.auth.AuthPlugin.Companion.authProvider
import co.statu.rule.auth.AuthPlugin.Companion.databaseManager
import io.vertx.ext.web.RoutingContext

abstract class PanelApi : LoggedInApi() {
    private suspend fun updateLastPanelActivityTime(context: RoutingContext) {
        val jdbcPool = databaseManager.getConnectionPool()
        val userId = authProvider.getUserIdFromRoutingContext(context)

        userDao.updateLastPanelActivityTime(userId, jdbcPool)
    }

    override suspend fun onBeforeHandle(context: RoutingContext) {
        super.onBeforeHandle(context)

        if (!authProvider.hasAccessPanel(context)) {
            throw NoPermission()
        }

        updateLastPanelActivityTime(context)
    }
}