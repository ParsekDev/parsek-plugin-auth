package co.statu.rule.auth.db.migration

import co.statu.parsek.annotation.Migration
import co.statu.rule.database.DatabaseMigration
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool

@Migration
class DbMigration1To2(
    override val FROM_SCHEME_VERSION: Int = 1,
    override val SCHEME_VERSION: Int = 2,
    override val SCHEME_VERSION_INFO: String = "Remove billDetail column"
) : DatabaseMigration() {
    override val handlers: List<suspend (jdbcPool: Pool, tablePrefix: String) -> Unit> = listOf(
        removeBillDetailColumn()
    )

    private fun removeBillDetailColumn(): suspend (jdbcPool: Pool, tablePrefix: String) -> Unit =
        { jdbcPool: Pool, tablePrefix: String ->
            jdbcPool
                .query("ALTER TABLE `${tablePrefix}user` DROP COLUMN `billDetail`;")
                .execute()
                .coAwait()
        }
}