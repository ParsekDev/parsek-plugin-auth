package co.statu.rule.auth.mail

import co.statu.rule.database.DatabaseManager
import co.statu.rule.mail.Mail
import co.statu.rule.token.provider.TokenProvider
import io.vertx.core.json.JsonObject
import io.vertx.jdbcclient.JDBCPool
import java.util.*

class DeletedAccountMail : Mail {
    override val templatePath = "account-deleted.hbs"
    override val subject = "Hesabınız Başarıyla Silindi - ${getBrandName()}"

    override suspend fun parameterGenerator(
        email: String,
        userId: UUID,
        uiAddress: String,
        databaseManager: DatabaseManager,
        jdbcPool: JDBCPool,
        tokenProvider: TokenProvider
    ): JsonObject {
        return JsonObject()
    }
}