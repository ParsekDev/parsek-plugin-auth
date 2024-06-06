package co.statu.rule.auth.db.migration

import co.statu.rule.database.DatabaseMigration
import io.vertx.core.json.JsonObject
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Tuple

class DbMigration4To5(
    override val FROM_SCHEME_VERSION: Int = 4,
    override val SCHEME_VERSION: Int = 5,
    override val SCHEME_VERSION_INFO: String = "Remove name, surname, fullName and lang columns and add additionalFields column to user table"
) : DatabaseMigration() {
    override val handlers: List<suspend (jdbcPool: JDBCPool, tablePrefix: String) -> Unit> = listOf(
        addAdditionalFieldsColumnToUserTable(),
        migrateFieldsToAdditionalFieldsColumn(),
        dropNameColumn(),
        dropSurnameColumn(),
        dropFullNameColumn(),
        dropLangColumn()
    )

    private fun addAdditionalFieldsColumnToUserTable(): suspend (jdbcPool: JDBCPool, tablePrefix: String) -> Unit =
        { jdbcPool: JDBCPool, tablePrefix: String ->
            jdbcPool
                .query("ALTER TABLE `${tablePrefix}user` ADD COLUMN `additionalFields` String DEFAULT '{}';")
                .execute()
                .await()
        }

    private fun migrateFieldsToAdditionalFieldsColumn(): suspend (jdbcPool: JDBCPool, tablePrefix: String) -> Unit =
        { jdbcPool: JDBCPool, tablePrefix: String ->
            val users = jdbcPool.query("SELECT * FROM `${tablePrefix}user`").execute().await()

            users.forEach { user ->
                val additionalFields = JsonObject()

                additionalFields.put("name", user.getString("name"))
                additionalFields.put("surname", user.getString("surname"))
                additionalFields.put("lang", user.getString("lang"))

                jdbcPool
                    .preparedQuery("ALTER TABLE `${tablePrefix}user` UPDATE `additionalFields` = ? WHERE `id` = ?;")
                    .execute(Tuple.of(additionalFields.encode(), user.getUUID("id")))
                    .coAwait()
            }
        }

    private fun dropNameColumn(): suspend (jdbcPool: JDBCPool, tablePrefix: String) -> Unit =
        { jdbcPool: JDBCPool, tablePrefix: String ->
            jdbcPool
                .query("ALTER TABLE `${tablePrefix}user` DROP COLUMN `name`;")
                .execute()
                .await()
        }

    private fun dropSurnameColumn(): suspend (jdbcPool: JDBCPool, tablePrefix: String) -> Unit =
        { jdbcPool: JDBCPool, tablePrefix: String ->
            jdbcPool
                .query("ALTER TABLE `${tablePrefix}user` DROP COLUMN `surname`;")
                .execute()
                .await()
        }

    private fun dropFullNameColumn(): suspend (jdbcPool: JDBCPool, tablePrefix: String) -> Unit =
        { jdbcPool: JDBCPool, tablePrefix: String ->
            jdbcPool
                .query("ALTER TABLE `${tablePrefix}user` DROP COLUMN `fullName`;")
                .execute()
                .await()
        }

    private fun dropLangColumn(): suspend (jdbcPool: JDBCPool, tablePrefix: String) -> Unit =
        { jdbcPool: JDBCPool, tablePrefix: String ->
            jdbcPool
                .query("ALTER TABLE `${tablePrefix}user` DROP COLUMN `lang`;")
                .execute()
                .await()
        }
}