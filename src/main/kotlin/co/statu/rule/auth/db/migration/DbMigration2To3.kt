package co.statu.rule.auth.db.migration

import co.statu.parsek.annotation.Migration
import co.statu.rule.database.DatabaseMigration
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool

@Migration
class DbMigration2To3(
    override val FROM_SCHEME_VERSION: Int = 2,
    override val SCHEME_VERSION: Int = 3,
    override val SCHEME_VERSION_INFO: String = "Add lang column"
) : DatabaseMigration() {
    override val handlers: List<suspend (jdbcPool: Pool, tablePrefix: String) -> Unit> = listOf(
        addLangColumnToUserTable()
    )

    private fun addLangColumnToUserTable(): suspend (jdbcPool: Pool, tablePrefix: String) -> Unit =
        { jdbcPool: Pool, tablePrefix: String ->
            jdbcPool
                .query("ALTER TABLE `${tablePrefix}user` ADD COLUMN `lang` String DEFAULT 'EN';")
                .execute()
                .coAwait()
        }
}