package co.statu.rule.auth.db.impl

import co.statu.parsek.api.ParsekPlugin
import co.statu.rule.auth.db.dao.PermissionGroupPermsDao
import co.statu.rule.auth.db.model.PermissionGroupPerms
import co.statu.rule.database.annotation.Dao
import io.vertx.sqlclient.Pool
import io.vertx.kotlin.coroutines.*
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope
import java.util.*

@Dao
@Lazy
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class PermissionGroupPermsDaoImpl : PermissionGroupPermsDao() {
    override suspend fun init(jdbcPool: Pool, plugin: ParsekPlugin) {
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
            .coAwait()
    }

    override suspend fun getPermissionGroupPerms(
        jdbcPool: Pool
    ): List<PermissionGroupPerms> {
        val query =
            "SELECT `id`, `permissionId`, `permissionGroupId` FROM `${getTablePrefix() + tableName}`"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute()
            .coAwait()

        return rows.toEntities()
    }

    override suspend fun getPermissionGroupPermsByPermissionId(
        permissionId: UUID,
        jdbcPool: Pool
    ): List<PermissionGroupPerms> {
        val query =
            "SELECT `id`, `permissionId`, `permissionGroupId` FROM `${getTablePrefix() + tableName}` WHERE `permissionId` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(permissionId))
            .coAwait()

        return rows.toEntities()
    }

    override suspend fun doesPermissionGroupHavePermission(
        permissionGroupId: UUID,
        permissionId: UUID,
        jdbcPool: Pool
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
            ).coAwait()

        return rows.toList()[0].getLong(0) != 0L
    }

    override suspend fun addPermission(
        permissionGroupId: UUID,
        permissionId: UUID,
        jdbcPool: Pool
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
            ).coAwait()
    }

    override suspend fun removePermission(
        permissionGroupId: UUID,
        permissionId: UUID,
        jdbcPool: Pool
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
            ).coAwait()
    }

    override suspend fun removePermissionGroup(
        permissionGroupId: UUID,
        jdbcPool: Pool,
    ) {
        val query =
            "DELETE FROM `${getTablePrefix() + tableName}` WHERE `permissionGroupId` = ?"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permissionGroupId
                )
            ).coAwait()
    }
}