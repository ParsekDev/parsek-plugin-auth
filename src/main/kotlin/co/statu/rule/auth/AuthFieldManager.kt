package co.statu.rule.auth

import co.statu.parsek.PluginEventManager
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.rule.auth.db.dao.UserDao
import co.statu.rule.auth.db.impl.UserDaoImpl
import co.statu.rule.auth.event.AuthEventListener
import co.statu.rule.database.DatabaseManager
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class AuthFieldManager(
    private val authPlugin: AuthPlugin,
) {
    private val pluginConfigManager by lazy {
        authPlugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<AuthConfig>
    }
    private val databaseManager by lazy {
        authPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    private val registerFields = mutableListOf<AuthConfig.Companion.RegisterField>()

    private val userDao: UserDao = UserDaoImpl()

    internal suspend fun init() {
        val config = pluginConfigManager.config

        registerFields.addAll(config.registerFields)

        val authEventHandlers = PluginEventManager.getEventListeners<AuthEventListener>()

        authEventHandlers.forEach { authEventHandler ->
            authEventHandler.onAuthFieldsManagerReady(this@AuthFieldManager)
        }
    }

    fun throwRegisterInputError(field: String, prefix: String = "Invalid", suffix: String = "") {
        throw RegisterInputError(prefix, suffix, field)
    }

    fun getRegisterFields() = registerFields.toList()

    fun addRegisterField(registerField: AuthConfig.Companion.RegisterField) {
        registerFields.add(registerField)
    }

    fun removeRegisterField(registerField: AuthConfig.Companion.RegisterField) {
        registerFields.remove(registerField)
    }

    suspend fun validateFields(
        fields: JsonObject,
        additionalFields: JsonObject = JsonObject(),
        register: Boolean = false
    ) {
        registerFields.forEach { registerField ->
            val field = fields.get<Any?>(registerField.field)

            if ((registerField.onlyRegister && register) || !registerField.onlyRegister) {
                validateField(registerField, field, additionalFields[registerField.field], !register)
            }
        }
    }

    suspend fun validateField(
        registerField: AuthConfig.Companion.RegisterField,
        field: Any?,
        additionalField: Any? = null,
        update: Boolean = false
    ) {
        try {
            if (update && field == null) {
                return
            }

            if (registerField.optional == false && field == null) {
                throwRegisterInputError(registerField.field)
            }

            val minString = registerField.min?.toString()
            val maxString = registerField.max?.toString()
            val connectionPool = databaseManager.getConnectionPool()

            if (registerField.type == AuthConfig.Companion.RegisterField.Companion.Type.STRING && (
                        field !is String
                                || (minString != null && field.length < (minString.toDoubleOrNull()?.toInt()
                            ?: minString.toInt()))
                                || (maxString != null && field.length > (maxString.toDoubleOrNull()?.toInt()
                            ?: maxString.toInt()))
                                || (registerField.isBlankCheck && field.isBlank())
                                || (registerField.regex != null && !Regex(registerField.regex).matches(field))
                        )
            ) {
                throwRegisterInputError(registerField.field)
            }

            if (
                registerField.type == AuthConfig.Companion.RegisterField.Companion.Type.STRING
                && registerField.unique
                && !userDao.isAdditionalFieldUnique(
                    registerField.field,
                    field as String,
                    connectionPool
                )
                && additionalField.toString() != field
            ) {
                throwRegisterInputError(registerField.field, "", "Exists")
            }

            if (registerField.type == AuthConfig.Companion.RegisterField.Companion.Type.INT && (
                        field !is Int
                                || (minString != null && field < (minString.toDoubleOrNull()?.toInt()
                            ?: minString.toInt()))
                                || (maxString != null && field > (maxString.toDoubleOrNull()?.toInt()
                            ?: maxString.toInt()))
                        )
            ) {
                throwRegisterInputError(registerField.field)
            }

            if (registerField.type == AuthConfig.Companion.RegisterField.Companion.Type.FLOAT && (
                        field !is Float
                                || (registerField.min != null && field < registerField.min.toString().toFloat())
                                || (registerField.max != null && field > registerField.max.toString().toFloat())
                        )
            ) {
                throwRegisterInputError(registerField.field)
            }

            if (registerField.type == AuthConfig.Companion.RegisterField.Companion.Type.DOUBLE && (
                        field !is Double
                                || (registerField.min != null && field < registerField.min.toString().toDouble())
                                || (registerField.max != null && field > registerField.max.toString().toDouble())
                        )
            ) {
                throwRegisterInputError(registerField.field)
            }

            if (registerField.type == AuthConfig.Companion.RegisterField.Companion.Type.BOOLEAN && field !is Boolean) {
                throwRegisterInputError(registerField.field)
            }

            val eventHandlers = PluginEventManager.getEventListeners<AuthEventListener>()

            eventHandlers.forEach {
                it.onValidatingRegisterField(field, registerField, this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throwRegisterInputError(registerField.field)
        }
    }

    fun getAdditionalFields(body: JsonObject): JsonObject {
        val additionalFields = JsonObject()

        registerFields
            .filter { registerField ->
                body.get<Any?>(registerField.field) != null
            }
            .forEach { registerField ->
                var value = body.get<Any?>(registerField.field)

                if (value != null && registerField.type == AuthConfig.Companion.RegisterField.Companion.Type.STRING && registerField.upperCaseFirstChar == true) {
                    value = (value as String).replaceFirstChar(Char::uppercase)
                }

                additionalFields.put(registerField.field, value)
            }

        return additionalFields
    }
}