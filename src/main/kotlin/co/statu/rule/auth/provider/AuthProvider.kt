package co.statu.rule.auth.provider

import co.statu.parsek.Main
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.error.NoPermission
import co.statu.rule.auth.AuthConfig
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.GoogleRecaptcha
import co.statu.rule.auth.PanelPermission
import co.statu.rule.auth.db.dao.PermissionGroupDao
import co.statu.rule.auth.db.dao.UserDao
import co.statu.rule.auth.db.model.Permission
import co.statu.rule.auth.db.model.User
import co.statu.rule.auth.error.*
import co.statu.rule.auth.event.AuthEventListener
import co.statu.rule.auth.token.AuthenticationToken
import co.statu.rule.auth.util.CsrfTokenGenerator
import co.statu.rule.auth.util.StringUtil
import co.statu.rule.database.Dao.Companion.get
import co.statu.rule.database.DatabaseManager
import co.statu.rule.plugins.i18n.I18nSystem
import co.statu.rule.systemProperty.db.dao.SystemPropertyDao
import co.statu.rule.systemProperty.db.model.SystemProperty
import co.statu.rule.token.provider.TokenProvider
import io.vertx.core.http.Cookie
import io.vertx.core.http.CookieSameSite
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.ext.web.client.sendAwait
import org.apache.commons.validator.routines.EmailValidator
import java.util.*

class AuthProvider private constructor(
    private val databaseManager: DatabaseManager,
    private val tokenProvider: TokenProvider,
    private val pluginConfigManager: PluginConfigManager<AuthConfig>,
    private val googleRecaptcha: GoogleRecaptcha,
    private val i18nSystem: I18nSystem,
    private val webClient: WebClient
) {
    companion object {
        private const val HEADER_PREFIX = "Bearer "

        internal suspend fun create(
            databaseManager: DatabaseManager,
            tokenProvider: TokenProvider,
            pluginConfigManager: PluginConfigManager<AuthConfig>,
            googleRecaptcha: GoogleRecaptcha,
            i18nSystem: I18nSystem,
            webClient: WebClient
        ): AuthProvider {
            val authProvider = AuthProvider(
                databaseManager,
                tokenProvider,
                pluginConfigManager,
                googleRecaptcha,
                i18nSystem,
                webClient
            )

            val handlers = AuthPlugin.INSTANCE.context.pluginEventManager.getEventHandlers<AuthEventListener>()

            handlers.forEach {
                it.onReady(authProvider)
            }

            return authProvider
        }
    }

    private val userDao by lazy {
        get<UserDao>(AuthPlugin.tables)
    }

    private val permissionGroupDao by lazy {
        get<PermissionGroupDao>(AuthPlugin.tables)
    }

    private val systemPropertyDao by lazy {
        get<SystemPropertyDao>(AuthPlugin.externalTables)
    }

    private val authenticationToken by lazy {
        AuthenticationToken()
    }

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

    fun validateRegisterInput(
        name: String,
        surname: String,
        lang: String,
    ) {
        if (name.isBlank() || name.length < 2 || name.length > 32) {
            throw InvalidName()
        }

        if (surname.isBlank() || surname.length < 2 || surname.length > 32) {
            throw InvalidSurname()
        }

        if (lang.isBlank() || i18nSystem.getSupportedLocales().none { it == lang }) {
            throw InvalidLang()
        }
    }

    private suspend fun onRegisterSuccess(user: User) {
        val authEventHandlers = AuthPlugin.INSTANCE.context.pluginEventManager.getEventHandlers<AuthEventListener>()

        authEventHandlers.forEach {
            it.onRegistrationComplete(user)
        }
    }

    suspend fun register(
        name: String,
        surname: String,
        email: String,
        lang: String,
        remoteIP: String,
        isAdmin: Boolean = false,
        jdbcPool: JDBCPool,
    ): UUID {
        val user = User(
            name = name,
            surname = surname,
            email = email,
            registeredIp = remoteIP,
            lang = lang
        )
        val userId: UUID

        if (!isAdmin) {
            userId = userDao.add(user, jdbcPool)

            onRegisterSuccess(user)

            return userId
        }

        val adminPermissionGroupId = permissionGroupDao.getPermissionGroupIdByName(
            "admin",
            jdbcPool
        )!!

        val adminUser = User(
            name = name,
            surname = surname,
            email = email,
            registeredIp = remoteIP,
            permissionGroupId = adminPermissionGroupId
        )

        userId = userDao.add(adminUser, jdbcPool)

        val property = SystemProperty(option = "who_installed_user_id", value = userId.toString())

        val isPropertyExists = systemPropertyDao.isPropertyExists(
            property,
            jdbcPool
        )

        if (isPropertyExists) {
            systemPropertyDao.update(
                property,
                jdbcPool
            )

            onRegisterSuccess(user)

            return userId
        }

        systemPropertyDao.add(
            property,
            jdbcPool
        )

        onRegisterSuccess(user)

        return userId
    }

    suspend fun login(
        email: String,
        jdbcPool: JDBCPool
    ): Pair<String, String> {
        val userId = userDao.getUserIdFromEmail(
            email,
            jdbcPool
        )!!

        val (token, expireDate) = tokenProvider.generateToken(userId.toString(), authenticationToken)

        tokenProvider.saveToken(token, userId.toString(), authenticationToken, expireDate, jdbcPool)

        val csrfToken = CsrfTokenGenerator.nextToken()

        return Pair(token, csrfToken)
    }

    fun setCookies(
        routingContext: RoutingContext,
        authToken: String,
        csrfToken: String
    ): Boolean {
        val config = pluginConfigManager.config
        val cookieConfig = config.cookieConfig

        if (!cookieConfig.enabled) {
            return false
        }

        val response = routingContext.response()

        val authTokenCookie = Cookie.cookie(cookieConfig.prefix + cookieConfig.authTokenName, authToken)
        val csrfTokenCookie = Cookie.cookie(cookieConfig.prefix + cookieConfig.csrfTokenName, csrfToken)

        val environmentType = AuthPlugin.INSTANCE.context.environmentType

        if (environmentType == Main.Companion.EnvironmentType.DEVELOPMENT) {
            authTokenCookie.setSameSite(CookieSameSite.NONE)
            csrfTokenCookie.setSameSite(CookieSameSite.NONE)
        }

        cookieConfig.domain?.let { authTokenCookie.domain = it }
        authTokenCookie.setMaxAge(7776000)
        authTokenCookie.path = "/"
        authTokenCookie.isSecure = true
        authTokenCookie.isHttpOnly = true

        cookieConfig.domain?.let { csrfTokenCookie.domain = it }
        csrfTokenCookie.setMaxAge(7776000)
        csrfTokenCookie.path = "/"
        csrfTokenCookie.isSecure = true
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
        email: String,
        recaptcha: String
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
            val response = webClient.getAbs(config.whitelistUrl)
                .sendAwait()

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
                .addQueryParam("email", StringUtil.anonymizeEmail(email))
                .sendAwait()

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

        val cookiePairs = cookieHeader.split(";")
        for (cookiePair in cookiePairs) {
            val (name, value) = cookiePair.trim().split("=")
            cookies[name] = value
        }

        return cookies
    }

    fun getTokenFromRoutingContext(routingContext: RoutingContext): String? {
        val request = routingContext.request()

        val config = pluginConfigManager.config
        val cookieConfig = config.cookieConfig
        val cookieHeader = request.getHeader("cookie") ?: return null

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

        val cookieHeader = request.getHeader("cookie") ?: throw InvalidCSRF()

        val cookies = parseCookies(cookieHeader)

        val csrfCookie = cookies[cookieConfig.prefix + cookieConfig.csrfTokenName] ?: throw InvalidCSRF()

        val authorizationHeader = routingContext.request().getHeader(cookieConfig.csrfHeader) ?: throw InvalidCSRF()

        if (authorizationHeader != csrfCookie) {
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

    suspend fun getAdminList(jdbcPool: JDBCPool): List<String> {
        val adminPermissionId = permissionGroupDao.getPermissionGroupIdByName("admin", jdbcPool)!!

        val admins = userDao.getEmailsByPermissionGroupId(adminPermissionId, -1, jdbcPool)

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