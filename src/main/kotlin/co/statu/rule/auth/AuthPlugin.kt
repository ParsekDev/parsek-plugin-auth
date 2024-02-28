package co.statu.rule.auth

import co.statu.parsek.api.ParsekPlugin
import co.statu.parsek.api.PluginContext
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.rule.auth.db.impl.*
import co.statu.rule.auth.db.migration.DbMigration1To2
import co.statu.rule.auth.db.migration.DbMigration2To3
import co.statu.rule.auth.db.migration.DbMigration3To4
import co.statu.rule.auth.event.*
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.database.Dao
import co.statu.rule.database.DatabaseManager
import co.statu.rule.mail.MailManager
import co.statu.rule.plugins.i18n.I18nSystem
import co.statu.rule.token.db.impl.TokenDaoImpl
import co.statu.rule.token.provider.TokenProvider
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AuthPlugin(pluginContext: PluginContext) : ParsekPlugin(pluginContext) {
    companion object {
        internal val logger: Logger = LoggerFactory.getLogger(AuthPlugin::class.java)

        internal lateinit var pluginConfigManager: PluginConfigManager<AuthConfig>

        internal lateinit var INSTANCE: AuthPlugin

        internal val tables by lazy {
            mutableListOf<Dao<*>>(
                UserDaoImpl(),
                PermissionDaoImpl(),
                PermissionGroupDaoImpl(),
                PermissionGroupPermsDaoImpl(),
                InvitationCodeDaoImpl()
            )
        }
        internal val externalTables = mutableListOf<Dao<*>>(
            TokenDaoImpl()
        )
        internal val migrations by lazy {
            listOf(
                DbMigration1To2(),
                DbMigration2To3(),
                DbMigration3To4()
            )
        }

        internal lateinit var databaseManager: DatabaseManager

        internal lateinit var authProvider: AuthProvider

        internal lateinit var tokenProvider: TokenProvider

        internal lateinit var mailManager: MailManager

        internal lateinit var i18nSystem: I18nSystem

        internal lateinit var webClient: WebClient

        internal lateinit var invitationCodeSystem: InvitationCodeSystem
    }

    init {
        INSTANCE = this

        logger.info("Initialized instance")

        context.pluginEventManager.register(this, ParsekEventHandler())
        context.pluginEventManager.register(this, RouterEventHandler())
        context.pluginEventManager.register(this, DatabaseEventHandler())
        context.pluginEventManager.register(this, MailEventHandler())
        context.pluginEventManager.register(this, TokenEventHandler())
        context.pluginEventManager.register(this, SystemPropertyEventHandler())
        context.pluginEventManager.register(this, I18nEventHandler())

        logger.info("Registered events")

        webClient = WebClient.create(context.vertx, WebClientOptions())

        logger.info("Webclient created")
    }
}

