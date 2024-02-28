package co.statu.rule.auth.db.migration

import co.statu.rule.database.DatabaseMigration
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.await

class DbMigration2To3(
    override val FROM_SCHEME_VERSION: Int = 2,
    override val SCHEME_VERSION: Int = 3,
    override val SCHEME_VERSION_INFO: String = "Add lang column"
) : DatabaseMigration() {
    override val handlers: List<suspend (jdbcPool: JDBCPool, tablePrefix: String) -> Unit> = listOf(
        addLangColumnToUserTable()
    )

    private fun addLangColumnToUserTable(): suspend (jdbcPool: JDBCPool, tablePrefix: String) -> Unit =
        { jdbcPool: JDBCPool, tablePrefix: String ->
            jdbcPool
                .query("ALTER TABLE `${tablePrefix}user` ADD COLUMN `lang` String DEFAULT 'EN';")
                .execute()
                .await()
        }
}