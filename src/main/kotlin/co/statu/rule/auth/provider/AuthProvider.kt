package co.statu.rule.auth.provider

import co.statu.parsek.Main
import co.statu.parsek.PluginEventManager
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.error.NoPermission
import co.statu.rule.auth.*
import co.statu.rule.auth.db.dao.PermissionGroupDao
import co.statu.rule.auth.db.dao.UserDao
import co.statu.rule.auth.db.impl.PermissionGroupDaoImpl
import co.statu.rule.auth.db.impl.UserDaoImpl
import co.statu.rule.auth.db.model.Permission
import co.statu.rule.auth.db.model.User
import co.statu.rule.auth.error.*
import co.statu.rule.auth.event.AuthEventListener
import co.statu.rule.auth.token.AuthenticationToken
import co.statu.rule.auth.util.CsrfTokenGenerator
import co.statu.rule.auth.util.StringUtil
import co.statu.rule.database.DatabaseManager
import co.statu.rule.systemProperty.db.dao.SystemPropertyDao
import co.statu.rule.systemProperty.db.impl.SystemPropertyDaoImpl
import co.statu.rule.systemProperty.db.model.SystemProperty
import co.statu.rule.token.provider.TokenProvider
import io.vertx.core.http.Cookie
import io.vertx.core.http.CookieSameSite
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.ext.web.client.sendAwait
import org.apache.commons.validator.routines.EmailValidator
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class AuthProvider private constructor(
    private val authPlugin: AuthPlugin,
) {
    private val databaseManager by lazy {
        authPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    private val googleRecaptcha by lazy {
        authPlugin.pluginBeanContext.getBean(GoogleRecaptcha::class.java)
    }

    private val pluginConfigManager by lazy {
        authPlugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<AuthConfig>
    }

    private val tokenProvider by lazy {
        authPlugin.pluginBeanContext.getBean(TokenProvider::class.java)
    }

    private val webClient by lazy {
        authPlugin.pluginBeanContext.getBean(WebClient::class.java)
    }

    private val authFieldManager by lazy {
        authPlugin.pluginBeanContext.getBean(AuthFieldManager::class.java)
    }

    companion object {
        private const val HEADER_PREFIX = "Bearer "
    }

    private val userDao: UserDao = UserDaoImpl()

    private val permissionGroupDao: PermissionGroupDao = PermissionGroupDaoImpl()

    private val systemPropertyDao: SystemPropertyDao = SystemPropertyDaoImpl()

    private val authenticationToken = AuthenticationToken()

    /**
     * authenticate method validates input and checks if logs in
     * it will throw error if there is an error
     */
    suspend fun authenticate(
        email: String
    ) {
        val jdbcPool = databaseManager.getConnectionPool()

        val isEmailExists = userDao.isEmailExists(email, jdbcPool)

        if (!isEmailExists) {
            throw InvalidEmail()
        }

        val userId = userDao.getUserIdFromEmail(email, jdbcPool)!!

        val isActive = userDao.isActive(userId, jdbcPool)

        if (!isActive) {
            throw LoginUserIsNotActive()
        }
    }

    suspend fun validateRegisterInput(body: JsonObject) {
        authFieldManager.validateFields(body, register = true)
    }

    private suspend fun onRegisterSuccess(user: User) {
        val authEventHandlers = PluginEventManager.getEventListeners<AuthEventListener>()

        authEventHandlers.forEach {
            it.onRegistrationComplete(user)
        }
    }

    suspend fun register(
        email: String,
        data: JsonObject,
        remoteIP: String,
        isAdmin: Boolean = false,
        jdbcPool: JDBCPool,
    ): UUID {
        val additionalFields = authFieldManager.getAdditionalFields(data)

        val user = User(
            email = email,
            registeredIp = remoteIP,
            additionalFields = additionalFields
        )
        val userId: UUID

        if (!isAdmin) {
            userId = userDao.add(user, jdbcPool)

            onRegisterSuccess(user)

            return userId
        }

        val adminPermissionGroupId = permissionGroupDao.getPermissionGroupIdByName(
            "admin", jdbcPool
        )!!

        val adminUser = User(
            email = email,
            additionalFields = additionalFields,
            registeredIp = remoteIP,
            permissionGroupId = adminPermissionGroupId
        )

        userId = userDao.add(adminUser, jdbcPool)

        val property = SystemProperty(option = "who_installed_user_id", value = userId.toString())

        val isPropertyExists = systemPropertyDao.isPropertyExists(
            property, jdbcPool
        )

        if (isPropertyExists) {
            systemPropertyDao.update(
                property, jdbcPool
            )

            onRegisterSuccess(user)

            return userId
        }

        systemPropertyDao.add(
            property, jdbcPool
        )

        onRegisterSuccess(user)

        return userId
    }

    suspend fun login(
        email: String, jdbcPool: JDBCPool
    ): Pair<String, String> {
        val userId = userDao.getUserIdFromEmail(
            email, jdbcPool
        )!!

        val (token, expireDate) = tokenProvider.generateToken(userId.toString(), authenticationToken)

        tokenProvider.saveToken(token, userId.toString(), authenticationToken, expireDate, jdbcPool = jdbcPool)

        val csrfToken = CsrfTokenGenerator.nextToken()

        return Pair(token, csrfToken)
    }

    fun setCookies(
        routingContext: RoutingContext, authToken: String, csrfToken: String
    ): Boolean {
        val config = pluginConfigManager.config
        val cookieConfig = config.cookieConfig

        if (!cookieConfig.enabled) {
            return false
        }

        val response = routingContext.response()

        val authTokenCookie = Cookie.cookie(cookieConfig.prefix + cookieConfig.authTokenName, authToken)
        val csrfTokenCookie = Cookie.cookie(cookieConfig.prefix + cookieConfig.csrfTokenName, csrfToken)

        val environmentType = authPlugin.environmentType

        if (environmentType == Main.Companion.EnvironmentType.DEVELOPMENT) {
            authTokenCookie.setSameSite(CookieSameSite.NONE)
            csrfTokenCookie.setSameSite(CookieSameSite.NONE)
        }

        cookieConfig.domain?.let { authTokenCookie.domain = it }
        authTokenCookie.setMaxAge(7776000)
        authTokenCookie.path = "/"
        authTokenCookie.isSecure = cookieConfig.secure
        authTokenCookie.isHttpOnly = true

        cookieConfig.domain?.let { csrfTokenCookie.domain = it }
        csrfTokenCookie.setMaxAge(7776000)
        csrfTokenCookie.path = "/"
        csrfTokenCookie.isSecure = cookieConfig.secure
        csrfTokenCookie.isHttpOnly = true

        response.addCookie(authTokenCookie)
        response.addCookie(csrfTokenCookie)

        return true
    }

    suspend fun isLoggedIn(
        routingContext: RoutingContext
    ): Boolean {
        val token = getTokenFromRoutingContext(routingContext) ?: return false

        val isTokenValid = tokenProvider.isTokenValid(token, authenticationToken)

        return isTokenValid
    }

    suspend fun hasAccessPanel(
        routingContext: RoutingContext
    ): Boolean {
        val userId = getUserIdFromRoutingContext(routingContext)

        return hasPermission(userId, PanelPermission.ACCESS_PANEL, routingContext)
    }

    suspend fun validateLoginInput(
        email: String, recaptcha: String
    ) {
        if (email.isEmpty()) {
            throw InvalidEmail()
        }

        if (!EmailValidator.getInstance().isValid(email)) {
            throw InvalidEmail()
        }

        validateRecaptcha(recaptcha)

        val config = pluginConfigManager.config

        if (!config.whitelistUrl.isNullOrBlank()) {
            val response = webClient.getAbs(config.whitelistUrl).sendAwait()

            val bodyAsJsonObject = response.bodyAsJsonObject()

            val emails = bodyAsJsonObject.getJsonArray("emails")

            if (!emails.contains(email)) {
                throw EmailNotInWhitelist()
            }
        }
    }

    suspend fun checkTempMail(email: String) {
        val config = pluginConfigManager.config
        val tempMailCheckConfig = config.tempMailCheckConfig

        if (!tempMailCheckConfig.enabled) {
            return
        }

        try {
            val response = webClient.getAbs("https://disposable.debounce.io/")
                .addQueryParam("email", StringUtil.anonymizeEmail(email)).sendAwait()

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                return
            }

            val body = response.bodyAsJsonObject()

            val disposable = body.getString("disposable").toBoolean()

            if (!disposable) {
                return
            }

            throw InvalidEmail()
        } catch (e: Exception) {
            return
        }
    }

    fun validateRecaptcha(recaptcha: String) {
        val recaptchaConfig = pluginConfigManager.config.recaptchaConfig
        val isRecaptchaEnabled = recaptchaConfig.enabled

        if (isRecaptchaEnabled && (recaptcha.isBlank() || !googleRecaptcha.isValid(recaptcha))) {
            throw InvalidRecaptcha()
        }
    }

    fun getUserIdFromRoutingContext(routingContext: RoutingContext): UUID {
        val token = getTokenFromRoutingContext(routingContext)

        return getUserIdFromToken(token!!)
    }

    fun getUserIdFromToken(token: String): UUID {
        val jwt = tokenProvider.parseToken(token)

        return UUID.fromString(jwt.subject)
    }

    private fun parseCookies(cookieHeader: String): Map<String, String> {
        val cookies = mutableMapOf<String, String>()

        try {
            val cookiePairs = cookieHeader.split(";")
            for (cookiePair in cookiePairs) {
                val (name, value) = cookiePair.trim().split("=")
                cookies[name] = value
            }
        } catch (_: Exception) {
        }

        return cookies
    }

    fun getTokenFromRoutingContext(routingContext: RoutingContext): String? {
        val request = routingContext.request()

        val config = pluginConfigManager.config
        val cookieConfig = config.cookieConfig
        val cookieHeader = request.getHeader("cookie") ?: ""

        val cookies = parseCookies(cookieHeader)

        val jwtCookie = cookies[cookieConfig.prefix + cookieConfig.authTokenName]

        if (jwtCookie != null) {
            return jwtCookie
        }

        val authorizationHeader = routingContext.request().getHeader("Authorization") ?: return null

        if (!authorizationHeader.contains(HEADER_PREFIX)) {
            return null
        }

        val splitHeader = authorizationHeader.split(HEADER_PREFIX)

        if (splitHeader.size != 2) {
            return null
        }

        return try {
            val token = splitHeader.last()

            token
        } catch (exception: Exception) {
            null
        }
    }

    fun validateCsrfToken(routingContext: RoutingContext) {
        val request = routingContext.request()

        val config = pluginConfigManager.config
        val cookieConfig = config.cookieConfig

        if (!cookieConfig.enabled) {
            return
        }

        val authorizationHeader = routingContext.request().getHeader("Authorization")

        if (authorizationHeader != null) {
            return
        }

        val cookieHeader = request.getHeader("cookie") ?: throw InvalidCSRF()

        val cookies = parseCookies(cookieHeader)

        val csrfCookie = cookies[cookieConfig.prefix + cookieConfig.csrfTokenName] ?: throw InvalidCSRF()

        val csrfHeader = routingContext.request().getHeader(cookieConfig.csrfHeader) ?: throw InvalidCSRF()

        if (csrfHeader != csrfCookie) {
            throw InvalidCSRF()
        }
    }

    suspend fun logout(routingContext: RoutingContext) {
        val isLoggedIn = isLoggedIn(routingContext)

        if (!isLoggedIn) {
            return
        }

        val token = getTokenFromRoutingContext(routingContext)!!

        tokenProvider.invalidateToken(token)
    }

    suspend fun getAdminList(jdbcPool: JDBCPool): List<User> {
        val adminPermissionId = permissionGroupDao.getPermissionGroupIdByName("admin", jdbcPool)!!

        val admins = userDao.getByPermissionGroupId(adminPermissionId, -1, jdbcPool)

        return admins
    }

    suspend fun hasPermission(userId: UUID, panelPermission: PanelPermission, context: RoutingContext): Boolean {
        val isAdmin = context.get<Boolean>("isAdmin")

        if (isAdmin != null) {
            return isAdmin
        }

        val existingPermissionsList = context.get<List<Permission>>("permissions")

        if (existingPermissionsList != null) {
            return existingPermissionsList.hasPermission(panelPermission)
        }

        val jdbcPool = databaseManager.getConnectionPool()

        val permissionGroupName = userDao.getPermissionGroupNameById(userId, jdbcPool)

        if (permissionGroupName == "admin") {
            context.put("isAdmin", true)

            return true
        }

        val permissions = userDao.getPermissionsById(userId, jdbcPool)

        context.put("permissions", permissions)

        return permissions.hasPermission(panelPermission)
    }

    suspend fun requirePermission(panelPermission: PanelPermission, context: RoutingContext) {
        val userId = getUserIdFromRoutingContext(context)

        if (!hasPermission(userId, panelPermission, context)) {
            throw NoPermission()
        }
    }

    private fun List<Permission>.hasPermission(panelPermission: PanelPermission) =
        this.any { it.name == panelPermission.toString() }

    suspend fun getUser(userId: UUID): User? {
        val jdbcPool = databaseManager.getConnectionPool()

        return userDao.byId(userId, jdbcPool)
    }
}