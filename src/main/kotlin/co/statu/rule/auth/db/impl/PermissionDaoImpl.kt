package co.statu.rule.auth.db.impl

import co.statu.parsek.api.ParsekPlugin
import co.statu.rule.auth.db.dao.PermissionDao
import co.statu.rule.auth.db.model.Permission
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import java.util.*

class PermissionDaoImpl : PermissionDao() {
    override suspend fun init(jdbcPool: JDBCPool, plugin: ParsekPlugin) {
        jdbcPool
            .query(
                """
                        CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                            `id` UUID NOT NULL,
                            `name` String NOT NULL
                        ) ENGINE = MergeTree() order by (`name`);
                        """
            )
            .execute()
            .await()

        val permissions = listOf(
            Permission(name = "access_panel"),
        )

        permissions.forEach { add(it, jdbcPool) }
    }

    override suspend fun isTherePermission(
        permission: Permission,
        jdbcPool: JDBCPool
    ): Boolean {
        val query =
            "SELECT COUNT(`name`) FROM `${getTablePrefix() + tableName}` where `name` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permission.name
                )
            ).await()

        return rows.toList()[0].getLong(0) != 0L
    }

    override suspend fun isTherePermissionById(
        id: UUID,
        jdbcPool: JDBCPool
    ): Boolean {
        val query =
            "SELECT COUNT(`id`) FROM `${getTablePrefix() + tableName}` where `id` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    id
                )
            ).await()

        return rows.toList()[0].getLong(0) != 0L
    }

    override suspend fun add(
        permission: Permission,
        jdbcPool: JDBCPool
    ) {
        val query = "INSERT INTO `${getTablePrefix() + tableName}` (`id`, `name`) VALUES (?, ?)"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permission.id,
                    permission.name
                )
            ).await()
    }

    override suspend fun getPermissionId(
        permission: Permission,
        jdbcPool: JDBCPool
    ): UUID {
        val query =
            "SELECT id FROM `${getTablePrefix() + tableName}` where `name` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permission.name
                )
            ).await()

        return rows.toList()[0].getUUID(0)
    }

    override suspend fun getPermissionById(
        id: UUID,
        jdbcPool: JDBCPool
    ): Permission? {
        val query =
            "SELECT `id`, `name` FROM `${getTablePrefix() + tableName}` where `id` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    id
                )
            ).await()

        if (rows.size() == 0) {
            return null
        }

        val row = rows.toList()[0]

        return row.toEntity()
    }

    override suspend fun getPermissions(
        jdbcPool: JDBCPool
    ): List<Permission> {
        val query =
            "SELECT `id`, `name` FROM `${getTablePrefix() + tableName}`"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute()
            .await()

        return rows.toEntities()
    }
}