package co.statu.rule.auth.db.impl

import co.statu.parsek.api.ParsekPlugin
import co.statu.rule.auth.db.dao.PermissionGroupPermsDao
import co.statu.rule.auth.db.model.PermissionGroupPerms
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import java.util.*

class PermissionGroupPermsDaoImpl : PermissionGroupPermsDao() {
    override suspend fun init(jdbcPool: JDBCPool, plugin: ParsekPlugin) {
        jdbcPool
            .query(
                """
                        CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                            `id` UUID NOT NULL,
                            `permissionId` UUID NOT NULL,
                            `permissionGroupId` UUID NOT NULL
                        ) ENGINE = MergeTree() order by `id`;
                        """
            )
            .execute()
            .await()
    }

    override suspend fun getPermissionGroupPerms(
        jdbcPool: JDBCPool
    ): List<PermissionGroupPerms> {
        val query =
            "SELECT `id`, `permissionId`, `permissionGroupId` FROM `${getTablePrefix() + tableName}`"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute()
            .await()

        return rows.toEntities()
    }

    override suspend fun getPermissionGroupPermsByPermissionId(
        permissionId: UUID,
        jdbcPool: JDBCPool
    ): List<PermissionGroupPerms> {
        val query =
            "SELECT `id`, `permissionId`, `permissionGroupId` FROM `${getTablePrefix() + tableName}` WHERE `permissionId` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(permissionId))
            .await()

        return rows.toEntities()
    }

    override suspend fun doesPermissionGroupHavePermission(
        permissionGroupId: UUID,
        permissionId: UUID,
        jdbcPool: JDBCPool
    ): Boolean {
        val query =
            "SELECT COUNT(`id`) FROM `${getTablePrefix() + tableName}` WHERE `permissionGroupId` = ? AND  `permissionId` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permissionGroupId,
                    permissionId
                )
            ).await()

        return rows.toList()[0].getLong(0) != 0L
    }

    override suspend fun addPermission(
        permissionGroupId: UUID,
        permissionId: UUID,
        jdbcPool: JDBCPool
    ) {
        val query =
            "INSERT INTO `${getTablePrefix() + tableName}` (`id`, `permissionId`, `permissionGroupId`) VALUES (generateUUIDv4(), ?, ?)"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permissionId,
                    permissionGroupId
                )
            ).await()
    }

    override suspend fun removePermission(
        permissionGroupId: UUID,
        permissionId: UUID,
        jdbcPool: JDBCPool
    ) {
        val query =
            "DELETE FROM `${getTablePrefix() + tableName}` WHERE `permissionGroupId` = ? AND `permissionId` = ?"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permissionGroupId,
                    permissionId
                )
            ).await()
    }

    override suspend fun removePermissionGroup(
        permissionGroupId: UUID,
        jdbcPool: JDBCPool,
    ) {
        val query =
            "DELETE FROM `${getTablePrefix() + tableName}` WHERE `permissionGroupId` = ?"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permissionGroupId
                )
            ).await()
    }
}