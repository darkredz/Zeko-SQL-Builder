package io.zeko.db.sql

import io.zeko.db.sql.utilities.toSnakeCase
import io.zeko.model.Entity

open class Insert : DataManipulation {
    protected var select: Query? = null
    protected var insertFields: List<String>? = null

    constructor(entity: Entity, parameterize: Boolean = false, espTableName: Boolean = false) {
        this.entity = entity
        this.parameterize = parameterize
        this.espTableName = espTableName
    }

    constructor(entity: Entity, vararg columns: String) {
        this.entity = entity
        this.insertFields = arrayListOf(*columns)
    }

    override fun escapeTable(espTableName: Boolean): Insert {
        super.escapeTable(espTableName)
        return this
    }

    fun select(query: Query): Insert {
        select = query
        return this
    }

    override fun toSql(): String {
        var sql = "INSERT INTO " + getTableName()
        var columns = arrayListOf<String>()
        var values = arrayListOf<String>()

        if (entity.dataMap().isNotEmpty()) {
            val entries = entity.dataMap().entries
            for ((propName, value) in entries) {
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
            sql += " ( " + columns.joinToString(", ") + " ) "
            sql += "VALUES ( " + values.joinToString(", ") + " ) "
        } else if (insertFields != null) {
            sql += " ( " + insertFields?.joinToString(", ") + " ) "
        } else {
            sql += " "
        }

        if (this.select != null) {
            sql += this.select?.toSql()
        }
        return sql.trimEnd()
    }
}
