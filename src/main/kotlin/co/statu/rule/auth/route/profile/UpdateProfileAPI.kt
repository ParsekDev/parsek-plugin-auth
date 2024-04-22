package co.statu.rule.auth.route.profile

import co.statu.parsek.annotation.Endpoint
import co.statu.parsek.model.*
import co.statu.rule.auth.AuthPlugin
import co.statu.rule.auth.api.LoggedInApi
import co.statu.rule.auth.error.*
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.database.DatabaseManager
import co.statu.rule.plugins.i18n.I18nSystem
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.RequestPredicate
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies.json
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas.objectSchema
import io.vertx.json.schema.common.dsl.Schemas.stringSchema
import org.apache.commons.validator.routines.EmailValidator

@Endpoint
class UpdateProfileAPI(
    private val authPlugin: AuthPlugin
) : LoggedInApi() {
    override val paths = listOf(Path("/profile", RouteType.PUT))

    private val databaseManager by lazy {
        authPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    private val i18nSystem by lazy {
        authPlugin.pluginBeanContext.getBean(I18nSystem::class.java)
    }

    private val authProvider by lazy {
        authPlugin.pluginBeanContext.getBean(AuthProvider::class.java)
    }

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .body(
                json(
                    objectSchema()
                        .optionalProperty("name", stringSchema())
                        .optionalProperty("surname", stringSchema())
                        .optionalProperty("email", stringSchema())
                        .optionalProperty("lang", stringSchema())
                )
            )
            .predicate(RequestPredicate.BODY_REQUIRED)
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val data = parameters.body().jsonObject

        val name = data.getString("name")
        val surname = data.getString("surname")
        val email = data.getString("email")
        val lang = data.getString("lang")

        val userId = authProvider.getUserIdFromRoutingContext(context)

        validateInput(
            name,
            surname,
            email,
            lang
        )

        val jdbcPool = databaseManager.getConnectionPool()

        val user = userDao.getById(userId, jdbcPool)!!

        if (email != null && email != user.email) {
            val emailExists = userDao.isEmailExists(email, jdbcPool)

            if (emailExists) {
                throw Errors(mapOf("email" to EmailNotAvailable()))
            }
        }

        if (name != null) {
            user.name = name
        }

        if (surname != null) {
            user.surname = surname
        }

        user.fullName = "${user.name} ${user.surname}"

        if (email != null) {
            user.email = email
        }

        if (lang != null) {
            user.lang = lang
        }

        userDao.update(user, jdbcPool)

        return Successful()
    }

    private fun validateInput(
        name: String?,
        surname: String?,
        email: String?,
        lang: String?
    ) {
        val errors = mutableMapOf<String, Error>()

        if (name != null && (name.isBlank() || name.length > 32 || name.length < 2)) {
            errors["name"] = InvalidName()
        }

        if (surname != null && (surname.isBlank() || surname.length > 32 || surname.length < 2)) {
            errors["surname"] = InvalidSurname()
        }

        if (email != null && (email.isBlank() || !EmailValidator.getInstance().isValid(email))) {
            errors["email"] = InvalidEmail()
        }

        if (lang != null && (lang.isBlank() || i18nSystem.getSupportedLocales().none { it == lang })) {
            errors["lang"] = InvalidLang()
        }

        if (errors.isNotEmpty()) {
            throw Errors(errors)
        }
    }
}