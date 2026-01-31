package co.statu.rule.auth

import co.statu.parsek.PluginEventManager
import co.statu.parsek.api.ParsekPlugin
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.rule.auth.event.AuthEventListener
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.database.DatabaseManager
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import org.springframework.beans.factory.getBean

class AuthPlugin : ParsekPlugin() {
    internal companion object {
        lateinit var authProvider: AuthProvider
            private set

        lateinit var databaseManager: DatabaseManager
            private set
    }

    private val invitationCodeSystem by lazy {
        pluginBeanContext.getBean<InvitationCodeSystem>()
    }

    override suspend fun onStart() {
        val webclient = WebClient.create(vertx, WebClientOptions())

        pluginBeanContext.beanFactory.registerSingleton(webclient.javaClass.name, webclient)

        databaseManager = pluginGlobalBeanContext.beanFactory.getBean<DatabaseManager>()
        databaseManager.initialize(this)

        val pluginConfigManager = PluginConfigManager(
            this,
            AuthConfig::class.java
        )

        pluginBeanContext.beanFactory.registerSingleton(
            pluginConfigManager.javaClass.name,
            pluginConfigManager
        )

        logger.info("Initialized plugin config")

        invitationCodeSystem.start()

        authProvider =
            pluginBeanContext.getBean<AuthProvider>()

        registerSingletonGlobal(authProvider)

        val handlers = PluginEventManager.getEventListeners<AuthEventListener>()

        handlers.forEach {
            it.onReady(authProvider)
        }

        val authFieldManager = pluginBeanContext.getBean<AuthFieldManager>()

        registerSingletonGlobal(authFieldManager)

        authFieldManager.init()
    }
}

