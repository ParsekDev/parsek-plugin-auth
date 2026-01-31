package co.statu.rule.auth.db.migration

import co.statu.rule.database.DatabaseMigration
import io.vertx.jdbcclient.JDBCPool
import io.vertx.sqlclient.Pool
import io.vertx.kotlin.coroutines.*

class DbMigration3To4(
    override val FROM_SCHEME_VERSION: Int = 3,
    override val SCHEME_VERSION: Int = 4,
    override val SCHEME_VERSION_INFO: String = "Create invitation code table"
) : DatabaseMigration() {
    override val handlers: List<suspend (jdbcPool: Pool, tablePrefix: String) -> Unit> = listOf(
        createInvitationCodeTable()
    )

    private fun createInvitationCodeTable(): suspend (jdbcPool: Pool, tablePrefix: String) -> Unit =
        { jdbcPool: Pool, tablePrefix: String ->
            jdbcPool
                .query(
                    """
                    CREATE TABLE IF NOT EXISTS `${tablePrefix}invitation_code` (
                            `id` UUID NOT NULL,
                            `code` String NOT NULL,
                            `usedByEmails` Array(String) DEFAULT [],
                            `usageLimit` Nullable(Int64),
                            `expiresAt` Nullable(Int64),
                            `createdAt` Int64 NOT NULL,
                            `updatedAt` Int64 NOT NULL
                        ) ENGINE = MergeTree() order by `createdAt`;
                """.trimIndent()
                )
                .execute()
                .coAwait()
        }
}