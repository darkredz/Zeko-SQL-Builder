package io.zeko.db.sql

import io.zeko.db.sql.utilities.toSnakeCase
import io.zeko.model.Entity

open class InsertBatch : BatchDataManipulation {
    protected var ignore = false
    protected var select: Query? = null
    protected var insertFields: List<String>? = null

    constructor(entities: List<Entity>, parameterize: Boolean = false, espTableName: Boolean = false) {
        this.entities = entities
        this.parameterize = parameterize
        this.espTableName = espTableName
    }

    constructor(entities: List<Entity>, vararg columns: String) {
        this.entities = entities
        this.insertFields = arrayListOf(*columns)
    }

    override fun escapeTable(espTableName: Boolean): InsertBatch {
        super.escapeTable(espTableName)
        return this
    }

    fun select(query: Query): InsertBatch {
        select = query
        return this
    }

    fun ignore(): InsertBatch {
        this.ignore = true
        return this
    }

    override fun toSql(): String {
        var sql = if (ignore)
            "INSERT IGNORE INTO " + getTableName()
        else
            "INSERT INTO " + getTableName()


        // loop through entities
        entities.forEach { entity ->
            val isFirstEntity = entities.indexOf(entity) == 0
            val columns = arrayListOf<String>()
            val values = arrayListOf<String>()

            if (entity.dataMap().isNotEmpty()) {
                val entries = entity.dataMap().entries
                val ignores = entity.ignoreFields()

                for ((propName, value) in entries) {
                    if (ignores.isNotEmpty() && ignores.indexOf(propName) > -1) continue
                    val prop = propName.toSnakeCase()
                    if (shouldIgnoreType(value)) continue
                    columns.add(prop)

                    if (parameterize) {
                        values.add("?")
                    } else {
                        if (value is String) {
                            values.add("'${value.replace("'", "''")}'")
                        } else {
                            values.add(value.toString())
                        }
                    }
                }

                if (isFirstEntity) {
                    sql += " ( " + columns.joinToString(", ") + " ) "
                    sql += "VALUES ( " + values.joinToString(", ") + " ) "
                } else {
                    sql += ", ( " + values.joinToString(", ") + " ) "
                }
            } else if (insertFields != null) {
                sql += " ( " + insertFields?.joinToString(", ") + " ) "
            } else {
                sql += " "
            }
        }

        if (this.select != null) {
            sql += this.select?.toSql()
        }

        return sql.trimEnd()
    }
}
