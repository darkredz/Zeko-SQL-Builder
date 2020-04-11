package io.zeko.db.sql

import io.zeko.model.Entity

open class Update : DataManipulation {
    protected var where: Query? = null

    constructor(entity: Entity, parameterize: Boolean = false, espTableName: Boolean = false) {
        this.entity = entity
        this.parameterize = parameterize
        this.espTableName = espTableName
    }

    override fun escapeTable(espTableName: Boolean): Update {
        super.escapeTable(espTableName)
        return this
    }

    fun where(query: Query): Update {
        where = query
        return this
    }

    fun where(vararg block: QueryBlock): Update {
        where = Query().where(*block)
        return this
    }

    override fun toSql(): String {
        var sql = "UPDATE ${getTableName()} SET "

        if (entity.dataMap().isNotEmpty()) {
            val entries = entity.dataMap().entries
            for ((prop, value) in entries) {
                if (shouldIgnoreType(value)) continue

                if (parameterize) {
                    sql += "$prop = ?, "
                } else {
                    if (value is String) {
                        sql += "$prop = '${value.replace("'", "''")}', "
                    } else {
                        sql += "$prop = $value, "
                    }
                }
            }
            sql = sql.substring(0, sql.length - 2)
        }

        if (this.where != null) {
            sql = sql.trimEnd()
            sql += " " + this.where?.toSql()?.replace("SELECT FROM ", "")
        }
        return sql.trimEnd()
    }
}
