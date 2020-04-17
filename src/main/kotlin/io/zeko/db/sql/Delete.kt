package io.zeko.db.sql

import io.zeko.db.sql.utilities.toSnakeCase
import io.zeko.model.Entity

open class Delete : DataManipulation {
    protected var where: Query? = null

    constructor(entity: Entity, parameterize: Boolean = false, espTableName: Boolean = false) {
        this.entity = entity
        this.parameterize = parameterize
        this.espTableName = espTableName
    }

    override fun escapeTable(espTableName: Boolean): Delete {
        super.escapeTable(espTableName)
        return this
    }

    fun where(query: Query): Delete {
        where = query
        return this
    }

    fun where(vararg block: QueryBlock): Delete {
        where = Query().where(*block)
        return this
    }

    override fun toSql(): String {
        var sql = "DELETE FROM ${getTableName()} "

        if (this.where != null) {
            sql += this.where?.toSql()?.replace("SELECT FROM ", "")
        } else {
            if (entity.dataMap().isNotEmpty()) {
                sql += "WHERE "
                val entries = entity.dataMap().entries

                for ((propName, value) in entries) {
                    if (shouldIgnoreType(value)) continue
                    val prop = propName.toSnakeCase()

                    if (parameterize) {
                        sql += "$prop = ? AND "
                    } else {
                        if (value is String) {
                            sql += "$prop = '${value.replace("'", "''")}' AND "
                        } else {
                            sql += "$prop = $value AND "
                        }
                    }
                }
                sql = sql.substring(0, sql.length - 4)
            }
        }
        return sql.trimEnd()
    }
}
