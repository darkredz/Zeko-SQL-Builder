package io.zeko.db.sql

import io.zeko.db.sql.utilities.toSnakeCase
import io.zeko.model.Entity

abstract class DataManipulation {
    protected lateinit var entity: Entity
    protected var parameterize = false
    protected var espTableName = false

    open fun escapeTable(espTableName: Boolean): DataManipulation {
        this.espTableName = espTableName
        return this
    }

    fun isTableNameEscaped(): Boolean {
        return this.espTableName
    }

    fun getTableName(): String {
        var table = (if (entity != null && !entity.tableName().isBlank()) entity.tableName() else "" + entity::class.simpleName?.toSnakeCase())
        if (this.espTableName) table = "\"$table\""
        return table
    }

    fun params(): List<Any> {
        val values = arrayListOf<Any>()
        val entries = entity.dataMap().entries
        for ((prop, value) in entries) {
            if (value != null) {
                values.add(value)
            }
        }
        return values
    }

    fun toMap(): MutableMap<String, Any?> {
        return entity.dataMap()
    }

    open fun shouldIgnoreType(value: Any?): Boolean {
        return when (value) {
            is List<*> -> true
            is Array<*> -> true
            is Map<*, *> -> true
            is Set<*> -> true
            is Entity -> true
            else -> false
        }
    }

    open fun toSql(): String = ""
}
