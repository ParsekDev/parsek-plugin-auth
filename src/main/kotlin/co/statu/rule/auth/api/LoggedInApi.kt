package co.statu.rule.auth.api

import co.statu.parsek.model.Api
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.AuthPlugin.Companion.authProvider
import co.statu.rule.auth.AuthPlugin.Companion.databaseManager
import co.statu.rule.auth.db.dao.UserDao
import co.statu.rule.auth.error.NotLoggedIn
import co.statu.rule.database.Dao.Companion.get
import io.vertx.ext.web.RoutingContext

abstract class LoggedInApi : Api() {
    val userDao by lazy {
        get<UserDao>(AuthPlugin.tables)
    }

    private suspend fun checkLoggedIn(context: RoutingContext) {
        val isLoggedIn = authProvider.isLoggedIn(context)

        if (!isLoggedIn) {
            throw NotLoggedIn()
        }
    }

    private suspend fun updateLastActivityTime(context: RoutingContext) {
        val userId = authProvider.getUserIdFromRoutingContext(context)
        val jdbcPool = databaseManager.getConnectionPool()

        userDao.updateLastActivityTime(userId, jdbcPool)
    }

    override suspend fun onBeforeHandle(context: RoutingContext) {
        checkLoggedIn(context)

        authProvider.validateCsrfToken(context)

        updateLastActivityTime(context)
    }
}