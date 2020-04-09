package io.zeko.db.sql

import java.util.*
import kotlin.collections.HashMap

data class QueryInfo(val sql: String, val columns: List<String>, val sqlFields: String, val parts: QueryParts)

class QueryParts {
    private val rgxFindField = "([^\\\"\\ ][a-zA-Z0-9\\_]+[^\\\"\\ ])\\.([^\\\"\\s][a-zA-Z0-9\\_\\-\\=\\`\\~\\:\\.\\,\\|\\*\\^\\#\\@\\\$]+[\\^\"\\s])".toPattern()
    private val rgxReplace = "\\\"\$1\\\".\$2"
    var linebreak: String = " "
        get() = field
        set(value) {
            field = value
        }

    var query:Query
    var fields: LinkedHashMap<String, Array<String>>
    var from: List<Any>
    var joins: LinkedHashMap<String, ArrayList<Condition>>
    var where: List<Condition>
    var groupBys: List<String>
    var havings: List<Condition>
    var order: List<Sort>
    var limit: Array<Int>?
    var custom: EnumMap<CustomPart, List<QueryBlock>>

    constructor(
            query:Query,
            fields: LinkedHashMap<String, Array<String>>,
            from: List<Any>,
            joins: LinkedHashMap<String, ArrayList<Condition>>,
            where: List<Condition>,
            order: List<Sort>,
            limit: Array<Int>?,
            groupBys: List<String>,
            havingCondition: List<Condition>,
            customExpression: EnumMap<CustomPart, List<QueryBlock>>
    ) {
        this.fields = fields
        this.query = query
        this.from = from
        this.joins = joins
        this.where = where
        this.order = order
        this.limit = limit
        this.groupBys = groupBys
        this.havings = havingCondition
        this.custom = customExpression
    }

    private fun escapeTableName(statement: String): String {
        val matcher = rgxFindField.matcher(statement)
        return matcher.replaceAll(rgxReplace)
    }

    override fun toString(): String {
        return compile()
    }

    private fun buildFromPart(esp: String, espTableName: Boolean): String {
        var fromPart = ""
        if (from.size > 0) {
            val f = from[0]

            if (f is String) {
                val fromTables = if (from.size == 1) from else from.distinct()
                fromTables.forEach {
                    val t = it.toString()
                    if (!t.isNullOrEmpty()) {
                        if (espTableName) {
                            fromPart += "${esp}${t}${esp}, "
                        } else {
                            fromPart += "$t, "
                        }
                    }
                }
            } else if (f is Query) {
                val subParts = f.compile()
                if (subParts.parts.from.size == 2) {
                    val subTable = subParts.parts.from[1]
                    val asTable = if (espTableName) "$esp$subTable$esp" else subTable
                    fromPart = "(${subParts.sql}) AS $asTable"
                    if (espTableName) {
                        fromPart = escapeTableName(fromPart)
                    }
                } else {
                    fromPart = "(${subParts.sql})"
                }
            }

            if (f is String) {
                fromPart = fromPart.substring(0, fromPart.length - 2)
            }
        }
        return fromPart
    }

    private fun buildJoinsPart(esp: String, espTableName: Boolean): String {
        var joinsPart = ""
        for ((join, conditions) in joins) {
            val parts = join.split("-")
            val tbl = if (espTableName) "$esp${parts.last()}$esp" else parts.last()
            val joinStmt = parts.subList(0, parts.size - 1).joinToString(" ").toUpperCase()
            var logicStmt = ""

            conditions.forEach {
                var s = " ${it.getStatement()} ${it.getOperator()} "
                val parts = s.split("=")
                if (parts.size > 0 && !parts[0].contains(".")) {
                    s = "${tbl}.${s.trimStart()}"
                }
                logicStmt += if (espTableName) escapeTableName(s) else s
            }

            if (logicStmt != "") {
                logicStmt = logicStmt.substring(0, logicStmt.length - 4)
                joinsPart += linebreak + "$joinStmt $tbl ON ($logicStmt)"
            }
        }
        return joinsPart
    }

    private fun buildWherePart(esp: String, espTableName: Boolean): String {
        var wherePart = ""
        where.forEach {
            val s = "${it.getStatement()} ${it.getOperator()} "
            wherePart += linebreak + (if (espTableName) escapeTableName(s) else s).trimEnd()
        }

        if (wherePart != "") {
            wherePart = linebreak + "WHERE" + wherePart.substring(0, wherePart.length - 3).replace("  ", " ")
        }
        return wherePart
    }

    private fun buildGroupByPart(esp: String, espTableName: Boolean): String {
        var groupByPart = ""
        if (groupBys.size > 0) {
            groupBys.forEach {
                if (espTableName) {
                    groupByPart += escapeTableName("$it, ")
                } else {
                    groupByPart += "$it, "
                }
            }

            groupByPart = linebreak + "GROUP BY " + groupByPart.substring(0, groupByPart.length - 2)
        }

        return groupByPart
    }

    private fun buildHavingPart(esp: String, espTableName: Boolean): String {
        var havingPart = ""
        havings.forEach {
            val s = "${it.getStatement()} ${it.getOperator()} "
            havingPart += linebreak + (if (espTableName) escapeTableName(s) else s).trimEnd()
        }

        if (havingPart != "") {
            havingPart = linebreak + "HAVING" + havingPart.substring(0, havingPart.length - 3).replace("  ", " ")
        }
        return havingPart
    }

    private fun buildOrderByPart(esp: String, espTableName: Boolean): String {
        var orderPart = ""
        order.forEach {
            val s = "${it.fieldName} ${it.getDirection()}, "
            orderPart += if (espTableName) escapeTableName(s) else s
        }

        if (orderPart != "") {
            orderPart = linebreak + "ORDER BY " + orderPart.substring(0, orderPart.length - 2)
        }
        return orderPart
    }

    private fun buildLimitOffsetPart(esp: String, espTableName: Boolean): String =
            linebreak + if (limit != null) "LIMIT ${limit!![0]} OFFSET ${limit!![1]} " else ""

    fun precompile(): Array<String> {
        val (columns, sqlFields) = query.compileSelect()
        val esp = query.espChar
        val espTableName = query.espTableName

        val fromPart = buildFromPart(esp, espTableName)
        val joinsPart = buildJoinsPart(esp, espTableName)
        val wherePart = buildWherePart(esp, espTableName)
        val groupByPart = buildGroupByPart(esp, espTableName)
        val havingPart = buildHavingPart(esp, espTableName)
        val orderPart = buildOrderByPart(esp, espTableName)
        val limitPart = buildLimitOffsetPart(esp, espTableName)

        return arrayOf(sqlFields, fromPart, joinsPart, wherePart, groupByPart, havingPart, orderPart, limitPart)
    }

    fun compile(): String {
        val allParts = precompile()
        val sqlFields = allParts[0]
        val others = allParts.slice(1 until allParts.size)

        val customParts = CustomPart.values()
        val partNames = customParts.slice(2 until customParts.size)
        var parts = ""

        others.forEachIndexed { idx, s ->
            val p = s.trim()
            if (p != "") parts += "$p "

            if (custom.containsKey(partNames[idx])) {
                val blocks = custom[partNames[idx]]
                blocks?.forEach {
                    parts += it.toString().trim() + " "
                }
            }
        }

        var sql = "SELECT "
        if (custom.containsKey(CustomPart.SELECT)) {
            val blocks = custom[CustomPart.SELECT]
            blocks?.forEach {
                sql += "$it "
            }
            sql = sql.replace("  ", " ")
        }

        sql += "$sqlFields "

        if (custom.containsKey(CustomPart.FIELD)) {
            val blocks = custom[CustomPart.SELECT]
            blocks?.forEach {
                sql += "$it "
            }
            sql = sql.replace("  ", " ")
        }

        sql = sql.trimEnd()
        return """$sql FROM $parts""".trimEnd()
    }
}
