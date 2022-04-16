package io.zeko.db.sql

import io.zeko.db.sql.utilities.toSnakeCase
import io.zeko.model.Entity

open class Insert : DataManipulation {
    protected var duplicateUpdateFields: Map<String, Any?>? = null
    protected var ignore = false
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

    fun ignore(): Insert {
        this.ignore = true
        return this
    }

    fun onDuplicateUpdate(fields: Map<String, Any?>?): Insert {
        duplicateUpdateFields = fields
        return this
    }

    override fun toSql(): String {
        var sql = if (ignore)
            "INSERT IGNORE INTO " + getTableName()
        else
            "INSERT INTO " + getTableName()

        var columns = arrayListOf<String>()
        var values = arrayListOf<String>()
        val onDuplicatePart = arrayListOf<String>()
        var onDuplicateSql = ""

        if (duplicateUpdateFields != null) {
            // ON DUPLICATE KEY UPDATE
            for ((propName, value) in duplicateUpdateFields!!) {
                if (value != null) {
                    if (parameterize) {
                        if (value is QueryBlock){
                            onDuplicatePart.add("$propName = $value")
                        } else {
                            onDuplicatePart.add("$propName = ?")
                        }
                    } else {
                        if (value is String) {
                            onDuplicatePart.add("$propName = '${value.replace("'", "''")}'")
                        } else if (value is QueryBlock){
                            onDuplicatePart.add("$propName = $value")
                        } else {
                            onDuplicatePart.add("$propName = $value")
                        }
                    }
                }
            }
            onDuplicateSql = " ON DUPLICATE KEY UPDATE " + onDuplicatePart.joinToString(", ")
        }

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
        sql += onDuplicateSql
        return sql.trimEnd()
    }
}
