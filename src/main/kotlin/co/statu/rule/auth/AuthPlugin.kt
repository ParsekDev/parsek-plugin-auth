package co.statu.rule.auth

import co.statu.parsek.api.ParsekPlugin
import co.statu.rule.auth.db.impl.*
import co.statu.rule.auth.db.migration.DbMigration1To2
import co.statu.rule.auth.db.migration.DbMigration2To3
import co.statu.rule.auth.db.migration.DbMigration3To4
import co.statu.rule.auth.db.migration.DbMigration4To5
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.database.Dao
import co.statu.rule.database.DatabaseManager
import co.statu.rule.database.DatabaseMigration
import co.statu.rule.database.api.DatabaseHelper
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions

class AuthPlugin : ParsekPlugin(), DatabaseHelper {
    internal companion object {
        lateinit var authProvider: AuthProvider

        lateinit var databaseManager: DatabaseManager
    }

    override suspend fun onLoad() {
        val webclient = WebClient.create(vertx, WebClientOptions())

        pluginBeanContext.beanFactory.registerSingleton(webclient.javaClass.name, webclient)
    }

    override val tables: List<Dao<*>> by lazy {
        mutableListOf<Dao<*>>(
            UserDaoImpl(),
            PermissionDaoImpl(),
            PermissionGroupDaoImpl(),
            PermissionGroupPermsDaoImpl(),
            InvitationCodeDaoImpl()
        )
    }

    override val migrations: List<DatabaseMigration> by lazy {
        listOf(
            DbMigration1To2(),
            DbMigration2To3(),
            DbMigration3To4(),
            DbMigration4To5()
        )
    }
}

