package io.zeko.db.sql

import java.util.*
import kotlin.collections.ArrayList


open class Query {
    var espChar: String
        get() = field
    var asChar: String
        get() = field
    var espTableName: Boolean
        get() = field

    private var currentTable: String = ""

    private val tableFrom = arrayListOf<Any>()

    private val fieldsToSelect by lazy {
        LinkedHashMap<String, Array<String>>()
    }

    private val tableToJoin by lazy {
        LinkedHashMap<String, ArrayList<Condition>>()
    }

    private val whereCondition by lazy {
        arrayListOf<Condition>()
    }

    private val groupBys by lazy {
        arrayListOf<String>()
    }

    private val havingCondition by lazy {
        arrayListOf<Condition>()
    }

    private val orderBy by lazy {
        arrayListOf<Sort>()
    }

    private val expression by lazy {
        EnumMap<CustomPart, List<QueryBlock>>(CustomPart::class.java)
    }

    private var limitOffset: Array<Int>? = null

    constructor(espChar: String =  "`", asChar: String = "=", espTableName: Boolean = false) {
        this.espChar = espChar
        this.asChar = asChar
        this.espTableName = espTableName
    }

    constructor(espChar: String =  "`", espTableName: Boolean = false) {
        this.espChar = espChar
        this.asChar = "="
        this.espTableName = espTableName
    }

    fun table(name: String): Query {
        currentTable = name
        return this
    }

    fun fields(vararg names: String): Query {
        fieldsToSelect[currentTable] = names as Array<String>
        return this
    }

    fun compileSelect(): SelectPart {
        val selectFields = mutableListOf<String>()
        val columns = mutableListOf<String>()

        for ((tbl, cols) in fieldsToSelect) {
            for (colName in cols) {
                if (colName.indexOf("=") != -1) {
                    val parts = colName.split(asChar)
                    val partField = parts[0].trim()
                    var tblLinkedCol: String
                    if (!espTableName) {
                        tblLinkedCol = partField
                    } else {
                        val fieldParts = partField.split(".")
                        val tblLinked = fieldParts[0]
                        tblLinkedCol = "${espChar}${tblLinked}${espChar}.${fieldParts[1]}"
                    }
                    val selfCol = parts[1].trim()
                    if (tbl == "") {
                        selectFields.add("$colName")
                    } else {
                        val aliasName = "$tbl-$selfCol"
                        columns.add(aliasName)
                        selectFields.add("$tblLinkedCol as $espChar$aliasName$espChar")
                    }
                } else {
                    if (tbl == "") {
                        selectFields.add("$colName")
                    } else {
                        val aliasName = "$tbl-$colName"
                        columns.add(aliasName)
                        val tblFinal = if (espTableName) "$espChar$tbl$espChar" else tbl
                        selectFields.add("$tblFinal.$colName as $espChar$aliasName$espChar")
                    }
                }
            }
        }

        val sqlFields = selectFields.joinToString(", ")
        return SelectPart(columns, sqlFields)
    }

    fun toParts(shouldLineBreak: Boolean = false): QueryParts {
        val parts = QueryParts(this, fieldsToSelect, tableFrom, tableToJoin, whereCondition, orderBy, limitOffset, groupBys, havingCondition, expression)
        if (shouldLineBreak) {
            parts.linebreak = "\n"
        }
        return parts
    }

    fun addExpressionAfter(type: CustomPart, block: QueryBlock) {
        if (this.expression.containsKey(type)) {
            (this.expression[type] as ArrayList).add(block)
        } else {
            this.expression[type] = arrayListOf(block)
        }
    }

    fun precompile(shouldLineBreak: Boolean = false): Array<String> {
        val selectPart = compileSelect()
        val parts = toParts(shouldLineBreak)
        return parts.precompile()
    }

    fun compile(processor: (Array<String>) -> String, shouldLineBreak: Boolean = false): QueryInfo {
        val selectPart = compileSelect()
        val parts = toParts(shouldLineBreak)
        val partsArr = parts.precompile()
        val sql = processor(partsArr)
        return QueryInfo(sql, selectPart.columns, selectPart.sqlFields, parts)
    }

    fun compile(shouldLineBreak: Boolean = false): QueryInfo {
        val selectPart = compileSelect()
        val parts = toParts(shouldLineBreak)
        return QueryInfo(parts.compile(), selectPart.columns, selectPart.sqlFields, parts)
    }

    fun toSql(shouldLineBreak: Boolean = false): String {
        return compile(shouldLineBreak).sql
    }

    override fun toString(): String {
        return compile().sql
    }

    fun from(table: String): Query {
        tableFrom.add(table)
        return this
    }

    fun from(tables: List<String>): Query {
        tableFrom.addAll(tables)
        return this
    }

    fun asTable(table: String): Query {
        tableFrom.add(table)
        return this
    }

    fun from(table: Query): Query {
        tableFrom.add(table)
        return this
    }

    fun join(table: String): Query {
        tableToJoin["join-" + table] = arrayListOf()
        return this
    }

    fun leftJoin(table: String): Query {
        tableToJoin["left-join-" + table] = arrayListOf()
        return this
    }

    fun leftOuterJoin(table: String): Query {
        tableToJoin["left-outer-join-" + table] = arrayListOf()
        return this
    }

    fun rightJoin(table: String): Query {
        tableToJoin["right-join-" + table] = arrayListOf()
        return this
    }

    fun rightOuterJoin(table: String): Query {
        tableToJoin["right-outer-join-" + table] = arrayListOf()
        return this
    }

    fun innerJoin(table: String): Query {
        tableToJoin["inner-join-" + table] = arrayListOf()
        return this
    }

    fun crossJoin(table: String): Query {
        tableToJoin["cross-join-" + table] = arrayListOf()
        return this
    }

    fun on(joinCondition: QueryBlock, useOr: Boolean = false): Query {
        return on(joinCondition.toString(), useOr)
    }

    fun on(joinCondition: String, useOr: Boolean = false): Query {
        if (tableToJoin.size > 0) {
            val tblName = tableToJoin.entries.last().key
            if (useOr) {
                tableToJoin[tblName]?.add(Or(joinCondition))
            } else {
                tableToJoin[tblName]?.add(And(joinCondition))
            }
        }
        return this
    }

    fun onAnd(joinCondition: String): Query {
        if (tableToJoin.size > 0) {
            val tblName = tableToJoin.entries.last().key
            tableToJoin[tblName]?.add(And(joinCondition))
        }
        return this
    }

    fun onOr(joinCondition: String): Query {
        if (tableToJoin.size > 0) {
            val tblName = tableToJoin.entries.last().key
            tableToJoin[tblName]?.add(Or(joinCondition))
        }
        return this
    }

    fun on(joinConditions: List<Condition>): Query {
        if (tableToJoin.size > 0) {
            val tblName = tableToJoin.entries.last().key
            tableToJoin[tblName]?.addAll(joinConditions)
        }
        return this
    }

    fun on(joinConditions: List<String>, useOr: Boolean = false): Query {
        if (tableToJoin.size > 0) {
            val tblName = tableToJoin.entries.last().key
            for (joinCondition in joinConditions) {
                if (useOr) {
                    tableToJoin[tblName]?.add(Or(joinCondition))
                } else {
                    tableToJoin[tblName]?.add(And(joinCondition))
                }
            }
        }
        return this
    }

    fun where(vararg blocks: QueryBlock): Query {
        return whereAnd(*blocks)
    }

    fun whereAnd(vararg blocks: QueryBlock): Query {
        (blocks as Array<QueryBlock>).forEach {
            whereCondition.add(And(it.toString()))
        }
        return this
    }

    fun whereOr(vararg blocks: QueryBlock): Query {
        (blocks as Array<QueryBlock>).forEach {
            whereCondition.add(Or(it.toString()))
        }
        return this
    }

    fun whereMix(vararg conditions: Any): Query {
        (conditions as Array<Any>).forEach {
            if (it is String) {
                where(it)
            } else if (it is QueryBlock){
                where(it)
            }
        }
        return this
    }

    fun where(vararg blocks: Any): Query {
        (blocks as Array<Any>).forEach {
            whereCondition.add(And(it.toString()))
        }
        return this
    }

    fun where(vararg blocks: String): Query {
        (blocks as Array<String>).forEach {
            whereCondition.add(And(it))
        }
        return this
    }

    fun where(queryBlock: QueryBlock, useOr: Boolean = false): Query {
        return where(queryBlock.toString(), useOr)
    }

    fun whereAnd(queryBlock: QueryBlock): Query {
        return whereAnd(queryBlock.toString())
    }

    fun whereOr(queryBlock: QueryBlock): Query {
        return whereOr(queryBlock.toString())
    }

    fun where(condition: String, useOr: Boolean = false): Query {
        if (useOr) {
            whereCondition.add(Or(condition))
        } else {
            whereCondition.add(And(condition))
        }
        return this
    }

    fun whereAnd(condition: String): Query {
        whereCondition.add(And(condition))
        return this
    }

    fun whereOr(condition: String): Query {
        whereCondition.add(Or(condition))
        return this
    }

    fun where(condition: List<Condition>): Query {
        whereCondition.addAll(condition)
        return this
    }

    fun where(conditions: List<String>, useOr: Boolean = false): Query {
        for (condition in conditions) {
            if (useOr) {
                whereCondition.add(Or(condition))
            } else {
                whereCondition.add(And(condition))
            }
        }
        return this
    }

    fun groupByMain(vararg fields: String): Query {
        addExpressionAfter(CustomPart.WHERE, QueryBlock("GROUP BY", if (fields.size == 1) fields[0] else fields.joinToString(", ")))
        return this
    }

    fun groupBy(vararg fields: String): Query {
        groupBys.addAll(fields as Array<String>)
        return this
    }

    fun groupBy(field: String): Query {
        groupBys.add(field)
        return this
    }

    fun groupBy(fields: List<String>): Query {
        groupBys.addAll(fields)
        return this
    }

    fun havingMix(vararg conditions: Any): Query {
        (conditions as Array<Any>).forEach {
            if (it is String) {
                having(it)
            } else if (it is QueryBlock){
                having(it)
            }
        }
        return this
    }

    fun having(vararg conditions: String): Query {
        return havingAnd(*conditions)
    }

    fun havingAnd(vararg conditions: String): Query {
        (conditions as Array<String>).forEach {
            havingCondition.add(And(it))
        }
        return this
    }

    fun havingOr(vararg conditions: String): Query {
        (conditions as Array<String>).forEach {
            havingCondition.add(Or(it))
        }
        return this
    }

    fun having(vararg blocks: QueryBlock): Query {
        return havingAnd(*blocks)
    }

    fun havingAnd(vararg blocks: QueryBlock): Query {
        (blocks as Array<QueryBlock>).forEach {
            havingCondition.add(And(it.toString()))
        }
        return this
    }

    fun havingOr(vararg blocks: QueryBlock): Query {
        (blocks as Array<QueryBlock>).forEach {
            havingCondition.add(Or(it.toString()))
        }
        return this
    }

    fun having(queryBlock: QueryBlock, useOr: Boolean = false): Query {
        return having(queryBlock.toString(), useOr)
    }

    fun havingAnd(queryBlock: QueryBlock): Query {
        return havingAnd(queryBlock.toString())
    }

    fun havingOr(queryBlock: QueryBlock): Query {
        return havingOr(queryBlock.toString())
    }

    fun having(condition: String, useOr: Boolean = false): Query {
        if (useOr) {
            havingCondition.add(Or(condition))
        } else {
            havingCondition.add(And(condition))
        }
        return this
    }

    fun havingAnd(condition: String): Query {
        havingCondition.add(And(condition))
        return this
    }

    fun havingOr(condition: String): Query {
        havingCondition.add(Or(condition))
        return this
    }

    fun having(condition: List<Condition>): Query {
        havingCondition.addAll(condition)
        return this
    }


    fun order(vararg fields: String): Query {
        return order(listOf(*fields))
    }

    fun order(vararg fields: Sort): Query {
        return order(listOf(*fields))
    }

    fun order(field: String, useDesc: Boolean = false): Query {
        if (useDesc) {
            orderBy.add(Desc(field))
        } else {
            orderBy.add(Asc(field))
        }
        return this
    }

    fun orderAsc(field: String): Query {
        orderBy.add(Asc(field))
        return this
    }

    fun orderDesc(field: String): Query {
        orderBy.add(Desc(field))
        return this
    }

    fun order(fields: List<Sort>): Query {
        orderBy.addAll(fields)
        return this
    }

    fun order(fields: List<String>, useDesc: Boolean = false): Query {
        for (field in fields) {
            if (useDesc) {
                orderBy.add(Desc(field))
            } else {
                orderBy.add(Asc(field))
            }
        }
        return this
    }

    fun limit(pageSize: Int, offset: Int = 0): Query {
        limitOffset = arrayOf(pageSize, offset)
        return this
    }

}
