package io.zeko.db.sql

import io.zeko.db.sql.utilities.toSnakeCase
import io.zeko.model.Entity

abstract class BatchDataManipulation {
    protected lateinit var entities: List<Entity>
    protected var parameterize = false
    protected var espTableName = false

    open fun escapeTable(espTableName: Boolean): BatchDataManipulation {
        this.espTableName = espTableName
        return this
    }

    fun isTableNameEscaped(): Boolean {
        return this.espTableName
    }

    fun getTableName(): String {
        val entity = entities[0]
        var table = (entity.tableName().ifBlank { "" + entity::class.simpleName?.toSnakeCase() })
        if (this.espTableName) table = "\"$table\""
        return table
    }

    fun params(): List<Any> {
        val values = arrayListOf<Any>()
        entities.forEach { entity ->
            val entries = entity.dataMap().entries
            for ((prop, value) in entries) {
                if (value != null) {
                    values.add(value)
                }
            }
        }
        return values
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
