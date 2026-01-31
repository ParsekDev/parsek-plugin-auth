package co.statu.rule.auth.db.impl

import co.statu.parsek.api.ParsekPlugin
import co.statu.rule.auth.db.dao.PermissionGroupDao
import co.statu.rule.auth.db.model.PermissionGroup
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
class PermissionGroupDaoImpl : PermissionGroupDao() {
    private val adminPermissionName = "admin"

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

        createAdminPermission(jdbcPool)
    }

    override suspend fun isThereByName(
        name: String,
        jdbcPool: Pool
    ): Boolean {
        val query =
            "SELECT COUNT(`name`) FROM `${getTablePrefix() + tableName}` where `name` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    name
                )
            ).coAwait()

        return rows.toList()[0].getLong(0) != 0L
    }

    override suspend fun isThere(permissionGroup: PermissionGroup, jdbcPool: Pool): Boolean {
        val query =
            "SELECT COUNT(`id`) FROM `${getTablePrefix() + tableName}` where `id` = ? and `name` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permissionGroup.id,
                    permissionGroup.name
                )
            ).coAwait()

        return rows.toList()[0].getLong(0) != 0L
    }

    override suspend fun isThereById(
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
        permissionGroup: PermissionGroup,
        jdbcPool: Pool
    ): UUID {
        val query = "INSERT INTO `${getTablePrefix() + tableName}` (`id`, `name`) VALUES (?, ?)"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permissionGroup.id,
                    permissionGroup.name
                )
            ).coAwait()

        return permissionGroup.id
    }

    override suspend fun getPermissionGroupById(
        id: UUID,
        jdbcPool: Pool
    ): PermissionGroup? {
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

    override suspend fun getPermissionGroupIdByName(
        name: String,
        jdbcPool: Pool
    ): UUID? {
        val query =
            "SELECT id FROM `${getTablePrefix() + tableName}` where `name` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    name
                )
            ).coAwait()

        if (rows.size() == 0) {
            return null
        }

        return rows.toList()[0].getUUID(0)
    }

    override suspend fun getPermissionGroups(
        jdbcPool: Pool
    ): List<PermissionGroup> {
        val query =
            "SELECT `id`, `name` FROM `${getTablePrefix() + tableName}` ORDER BY `ID` ASC"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute()
            .coAwait()

        return rows.toEntities()
    }

    override suspend fun deleteById(
        id: UUID,
        jdbcPool: Pool
    ) {
        val query =
            "DELETE FROM `${getTablePrefix() + tableName}` WHERE `id` = ?"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    id
                )
            )
            .coAwait()
    }

    override suspend fun update(
        permissionGroup: PermissionGroup,
        jdbcPool: Pool
    ) {
        val query =
            "UPDATE `${getTablePrefix() + tableName}` SET `name` = ? WHERE `id` = ?"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permissionGroup.name,
                    permissionGroup.id
                )
            )
            .coAwait()
    }

    override suspend fun getByListOfId(idList: Set<UUID>, jdbcPool: Pool): Map<UUID, PermissionGroup> {
        var listText = ""

        idList.forEach { id ->
            if (listText == "")
                listText = "'$id'"
            else
                listText += ", '$id'"
        }

        val query =
            "SELECT `id`, `name` FROM `${getTablePrefix() + tableName}` where `id` IN ($listText)"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute()
            .coAwait()

        val idPermissionGroupMap = mutableMapOf<UUID, PermissionGroup>()

        rows.forEach { row ->
            val permissionGroup = row.toEntity()

            idPermissionGroupMap[permissionGroup.id] = permissionGroup
        }

        return idPermissionGroupMap
    }

    private suspend fun createAdminPermission(
        jdbcPool: Pool
    ) {
        val isThere = isThereByName(adminPermissionName, jdbcPool)

        if (isThere) {
            return
        }

        add(PermissionGroup(name = adminPermissionName), jdbcPool)
    }
}