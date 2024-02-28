package co.statu.rule.auth.db.impl

import co.statu.parsek.api.ParsekPlugin
import co.statu.rule.auth.db.dao.InvitationCodeDao
import co.statu.rule.auth.db.model.InvitationCode
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import java.util.*

class InvitationCodeDaoImpl : InvitationCodeDao() {
    override suspend fun init(jdbcPool: JDBCPool, plugin: ParsekPlugin) {
        jdbcPool
            .query(
                """
                        CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                            `id` UUID NOT NULL,
                            `code` String NOT NULL,
                            `usedByEmails` Array(String) DEFAULT [],
                            `usageLimit` Nullable(Int64),
                            `expiresAt` Nullable(Int64),
                            `createdAt` Int64 NOT NULL,
                            `updatedAt` Int64 NOT NULL
                        ) ENGINE = MergeTree() order by `createdAt`;
                        """
            )
            .execute()
            .await()
    }

    override suspend fun add(
        invitationCode: InvitationCode,
        jdbcPool: JDBCPool
    ): UUID {
        val query =
            "INSERT INTO `${getTablePrefix() + tableName}` (${fields.toTableQuery()}) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    invitationCode.id,
                    invitationCode.code,
                    invitationCode.usedByEmails,
                    invitationCode.usageLimit,
                    invitationCode.expiresAt,
                    invitationCode.createdAt,
                    invitationCode.updatedAt
                )
            )
            .await()

        return invitationCode.id
    }

    override suspend fun addAll(invitationCodes: List<InvitationCode>, jdbcPool: JDBCPool): List<UUID> {
        if (invitationCodes.isEmpty()) {
            return listOf()
        }

        val patternText = "(" + (1..fields.size).joinToString(", ") { "?" } + ")"

        val batchTuple = invitationCodes.map { invitationCode ->
            Tuple.of(
                invitationCode.id,
                invitationCode.code,
                invitationCode.usedByEmails,
                invitationCode.usageLimit,
                invitationCode.expiresAt,
                invitationCode.createdAt,
                invitationCode.updatedAt
            )
        }

        val query =
            "INSERT INTO `${getTablePrefix() + tableName}` (${fields.toTableQuery()}) " +
                    "VALUES $patternText"

        jdbcPool
            .preparedQuery(query)
            .executeBatch(batchTuple)
            .await()

        return invitationCodes.map { it.id }
    }

    override suspend fun byCode(
        code: String,
        jdbcPool: JDBCPool
    ): InvitationCode? {
        val query =
            "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `code` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(code))
            .await()

        if (rows.size() == 0) {
            return null
        }

        val row = rows.toList()[0]

        return row.toEntity()
    }

    override suspend fun update(invitationCode: InvitationCode, jdbcPool: JDBCPool) {
        val query =
            "UPDATE `${getTablePrefix() + tableName}` SET `usedByEmails` = ?, `updatedAt` = ? WHERE `id` = ?"

        val parameters = Tuple.tuple()

        parameters.addArrayOfString(invitationCode.usedByEmails)

        parameters.addLong(System.currentTimeMillis())

        parameters.addUUID(invitationCode.id)

        jdbcPool
            .preparedQuery(query)
            .execute(parameters)
            .await()
    }
}