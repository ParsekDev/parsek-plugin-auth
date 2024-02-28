package co.statu.rule.auth.db.impl

import co.statu.parsek.api.ParsekPlugin
import co.statu.rule.auth.db.dao.PermissionGroupDao
import co.statu.rule.auth.db.model.PermissionGroup
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import java.util.*

class PermissionGroupDaoImpl : PermissionGroupDao() {
    private val adminPermissionName = "admin"

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

        createAdminPermission(jdbcPool)
    }

    override suspend fun isThereByName(
        name: String,
        jdbcPool: JDBCPool
    ): Boolean {
        val query =
            "SELECT COUNT(`name`) FROM `${getTablePrefix() + tableName}` where `name` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    name
                )
            ).await()

        return rows.toList()[0].getLong(0) != 0L
    }

    override suspend fun isThere(permissionGroup: PermissionGroup, jdbcPool: JDBCPool): Boolean {
        val query =
            "SELECT COUNT(`id`) FROM `${getTablePrefix() + tableName}` where `id` = ? and `name` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permissionGroup.id,
                    permissionGroup.name
                )
            ).await()

        return rows.toList()[0].getLong(0) != 0L
    }

    override suspend fun isThereById(
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
        permissionGroup: PermissionGroup,
        jdbcPool: JDBCPool
    ): UUID {
        val query = "INSERT INTO `${getTablePrefix() + tableName}` (`id`, `name`) VALUES (?, ?)"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    permissionGroup.id,
                    permissionGroup.name
                )
            ).await()

        return permissionGroup.id
    }

    override suspend fun getPermissionGroupById(
        id: UUID,
        jdbcPool: JDBCPool
    ): PermissionGroup? {
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

    override suspend fun getPermissionGroupIdByName(
        name: String,
        jdbcPool: JDBCPool
    ): UUID? {
        val query =
            "SELECT id FROM `${getTablePrefix() + tableName}` where `name` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    name
                )
            ).await()

        if (rows.size() == 0) {
            return null
        }

        return rows.toList()[0].getUUID(0)
    }

    override suspend fun getPermissionGroups(
        jdbcPool: JDBCPool
    ): List<PermissionGroup> {
        val query =
            "SELECT `id`, `name` FROM `${getTablePrefix() + tableName}` ORDER BY `ID` ASC"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute()
            .await()

        return rows.toEntities()
    }

    override suspend fun deleteById(
        id: UUID,
        jdbcPool: JDBCPool
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
            .await()
    }

    override suspend fun update(
        permissionGroup: PermissionGroup,
        jdbcPool: JDBCPool
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
            .await()
    }

    override suspend fun getByListOfId(idList: Set<UUID>, jdbcPool: JDBCPool): Map<UUID, PermissionGroup> {
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
            .await()

        val idPermissionGroupMap = mutableMapOf<UUID, PermissionGroup>()

        rows.forEach { row ->
            val permissionGroup = row.toEntity()

            idPermissionGroupMap[permissionGroup.id] = permissionGroup
        }

        return idPermissionGroupMap
    }

    private suspend fun createAdminPermission(
        jdbcPool: JDBCPool
    ) {
        val isThere = isThereByName(adminPermissionName, jdbcPool)

        if (isThere) {
            return
        }

        add(PermissionGroup(name = adminPermissionName), jdbcPool)
    }
}