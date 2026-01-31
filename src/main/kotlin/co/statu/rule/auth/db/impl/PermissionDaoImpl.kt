package co.statu.rule.auth.db.impl

import co.statu.parsek.api.ParsekPlugin
import co.statu.rule.auth.db.dao.PermissionDao
import co.statu.rule.auth.db.model.Permission
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
class PermissionDaoImpl : PermissionDao() {
    override suspend fun init(jdbcPool: Pool, plugin: ParsekPlugin) {
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
            .coAwait()

        val permissions = listOf(
            Permission(name = "access_panel"),
        )

        permissions.forEach { add(it, jdbcPool) }
    }

    override suspend fun isTherePermission(
        permission: Permission,
        jdbcPool: Pool
    ): Boolean {
        val query =
            "SELECT COUNT(`name`) FROM `${getTablePrefix() + tableName}` where `name` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permission.name
                )
            ).coAwait()

        return rows.toList()[0].getLong(0) != 0L
    }

    override suspend fun isTherePermissionById(
        id: UUID,
        jdbcPool: Pool
    ): Boolean {
        val query =
            "SELECT COUNT(`id`) FROM `${getTablePrefix() + tableName}` where `id` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    id
                )
            ).coAwait()

        return rows.toList()[0].getLong(0) != 0L
    }

    override suspend fun add(
        permission: Permission,
        jdbcPool: Pool
    ) {
        val query = "INSERT INTO `${getTablePrefix() + tableName}` (`id`, `name`) VALUES (?, ?)"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permission.id,
                    permission.name
                )
            ).coAwait()
    }

    override suspend fun getPermissionId(
        permission: Permission,
        jdbcPool: Pool
    ): UUID {
        val query =
            "SELECT id FROM `${getTablePrefix() + tableName}` where `name` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permission.name
                )
            ).coAwait()

        return rows.toList()[0].getUUID(0)
    }

    override suspend fun getPermissionById(
        id: UUID,
        jdbcPool: Pool
    ): Permission? {
        val query =
            "SELECT `id`, `name` FROM `${getTablePrefix() + tableName}` where `id` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    id
                )
            ).coAwait()

        if (rows.size() == 0) {
            return null
        }

        val row = rows.toList()[0]

        return row.toEntity()
    }

    override suspend fun getPermissions(
        jdbcPool: Pool
    ): List<Permission> {
        val query =
            "SELECT `id`, `name` FROM `${getTablePrefix() + tableName}`"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute()
            .coAwait()

        return rows.toEntities()
    }
}