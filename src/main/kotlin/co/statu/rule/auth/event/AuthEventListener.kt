package co.statu.rule.auth.event

import co.statu.parsek.api.event.PluginEventListener
import co.statu.rule.auth.db.model.User
import co.statu.rule.auth.provider.AuthProvider

interface AuthEventListener : PluginEventListener {

    suspend fun onReady(authProvider: AuthProvider) {}

    suspend fun onRegistrationComplete(user: User) {}

    suspend fun onGetProfile(user: User, response: MutableMap<String, Any?>) {}
}