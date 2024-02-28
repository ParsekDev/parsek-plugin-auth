package co.statu.rule.auth.db.dao

import co.statu.rule.auth.db.model.InvitationCode
import co.statu.rule.database.Dao
import io.vertx.jdbcclient.JDBCPool
import java.util.*

abstract class InvitationCodeDao : Dao<InvitationCode>(InvitationCode::class) {
    abstract suspend fun add(
        invitationCode: InvitationCode,
        jdbcPool: JDBCPool
    ): UUID

    abstract suspend fun addAll(
        invitationCodes: List<InvitationCode>,
        jdbcPool: JDBCPool
    ): List<UUID>

    abstract suspend fun byCode(
        code: String,
        jdbcPool: JDBCPool
    ): InvitationCode?

    abstract suspend fun update(
        invitationCode: InvitationCode,
        jdbcPool: JDBCPool
    )
}