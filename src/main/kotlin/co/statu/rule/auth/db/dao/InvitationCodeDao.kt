package co.statu.rule.auth.db.dao

import co.statu.rule.auth.db.model.InvitationCode
import co.statu.rule.database.Dao
import io.vertx.jdbcclient.JDBCPool
import io.vertx.sqlclient.Pool
import java.util.*

abstract class InvitationCodeDao : Dao<InvitationCode>(InvitationCode::class) {
    abstract suspend fun add(
        invitationCode: InvitationCode,
        jdbcPool: Pool
    ): UUID

    abstract suspend fun addAll(
        invitationCodes: List<InvitationCode>,
        jdbcPool: Pool
    ): List<UUID>

    abstract suspend fun byCode(
        code: String,
        jdbcPool: Pool
    ): InvitationCode?

    abstract suspend fun update(
        invitationCode: InvitationCode,
        jdbcPool: Pool
    )
}