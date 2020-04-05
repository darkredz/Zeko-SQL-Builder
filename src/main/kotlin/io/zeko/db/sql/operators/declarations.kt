package io.zeko.db.sql.operators

import io.zeko.db.sql.QueryBlock


fun eq(field: String, value: String): QueryBlock {
    return QueryBlock(field, "=", value)
}

fun neq(field: String, value: String): QueryBlock {
    return QueryBlock(field, "!=", value)
}

fun eq(field: String, value: Int): QueryBlock {
    return QueryBlock(field, "=", value.toString())
}

fun neq(field: String, value: Int): QueryBlock {
    return QueryBlock(field, "!=", value.toString())
}

fun eq(field: String, value: Double): QueryBlock {
    return QueryBlock(field, "=", value.toString())
}

fun neq(field: String, value: Double): QueryBlock {
    return QueryBlock(field, "!=", value.toString())
}

fun eq(field: String, value: Long): QueryBlock {
    return QueryBlock(field, "=", value.toString())
}

fun neq(field: String, value: Long): QueryBlock {
    return QueryBlock(field, "!=", value.toString())
}

fun greater(field: String, value: String): QueryBlock {
    return QueryBlock(field, ">", value)
}
fun greaterEq(field: String, value: String): QueryBlock {
    return QueryBlock(field, ">=", value)
}
fun less(field: String, value: String): QueryBlock {
    return QueryBlock(field, "<", value)
}
fun lessEq(field: String, value: String): QueryBlock {
    return QueryBlock(field, "<=", value)
}

fun greater(field: String, value: Int): QueryBlock {
    return QueryBlock(field, ">", value.toString())
}
fun greaterEq(field: String, value: Int): QueryBlock {
    return QueryBlock(field, ">=", value.toString())
}
fun less(field: String, value: Int): QueryBlock {
    return QueryBlock(field, "<", value.toString())
}
fun lessEq(field: String, value: Int): QueryBlock {
    return QueryBlock(field, "<=", value.toString())
}

fun greater(field: String, value: Long): QueryBlock {
    return QueryBlock(field, ">", value.toString())
}
fun greaterEq(field: String, value: Long): QueryBlock {
    return QueryBlock(field, ">=", value.toString())
}
fun less(field: String, value: Long): QueryBlock {
    return QueryBlock(field, "<", value.toString())
}
fun lessEq(field: String, value: Long): QueryBlock {
    return QueryBlock(field, "<=", value.toString())
}

fun greater(field: String, value: Double): QueryBlock {
    return QueryBlock(field, ">", value.toString())
}
fun greaterEq(field: String, value: Double): QueryBlock {
    return QueryBlock(field, ">=", value.toString())
}
fun less(field: String, value: Double): QueryBlock {
    return QueryBlock(field, "<", value.toString())
}
fun lessEq(field: String, value: Double): QueryBlock {
    return QueryBlock(field, "<=", value.toString())
}

fun like(field: String, value: String): QueryBlock {
    return QueryBlock(field, "LIKE", value)
}

fun notLike(field: String, value: String): QueryBlock {
    return QueryBlock(field, "NOT LIKE", value)
}

fun regexp(field: String, regex: String): QueryBlock {
    return QueryBlock("REGEX_LIKE( ", field, ", '$regex' )")
}

fun regexp(field: String, regex: String, regexOption: String): QueryBlock {
    return QueryBlock("REGEX_LIKE( ", field, ", '$regex', '$regexOption' )")
}

fun notRegexp(field: String, regex: String): QueryBlock {
    return QueryBlock("NOT REGEX_LIKE( ", field, ", '$regex' )")
}

fun notRegexp(field: String, regex: String, regexOption: String): QueryBlock {
    return QueryBlock("NOT REGEX_LIKE( ", field, ", '$regex', '$regexOption' )")
}

fun match(field: String, search: String, mode: String = "NATURAL LANGUAGE MODE"): QueryBlock {
    return QueryBlock("MATCH( ", field, ") AGAINST ( '$search' IN $mode )")
}

fun match(field: List<String>, search: String, mode: String = "NATURAL LANGUAGE MODE"): QueryBlock {
    return QueryBlock("MATCH( ", field.joinToString(","), ") AGAINST ( '$search' IN $mode )")
}

fun between(field: String, value1: String, value2: String): QueryBlock {
    return QueryBlock("$field BETWEEN ", value1, " AND $value2")
}

fun between(field: String, value1: Int, value2: Int): QueryBlock {
    return QueryBlock("$field BETWEEN ", value1.toString(), " AND $value2")
}


fun isNotNull(stmt: String): QueryBlock {
    return QueryBlock(stmt, "IS NOT NULL")
}
fun isNull(stmt: String): QueryBlock {
    return QueryBlock(stmt, "IS NULL")
}

fun inList(stmt: String, values: String): QueryBlock {
    return QueryBlock(stmt, "IN", "($values)")
}

fun inList(stmt: String, valuesSize: Int): QueryBlock {
    var valEsp = "?,".repeat(valuesSize)
    valEsp = valEsp.substring(0, valEsp.length - 1)
    return QueryBlock(stmt, "IN", "($valEsp)")
}

fun inList(stmt: String, values: List<Any>): QueryBlock {
    var valEsp = "?,".repeat(values.size)
    valEsp = valEsp.substring(0, valEsp.length - 1)
    return QueryBlock(stmt, "IN", "($valEsp)")
}

fun notInList(stmt: String, values: String): QueryBlock {
    return QueryBlock(stmt, "NOT IN", "($values)")
}

fun notInList(stmt: String, valuesSize: Int): QueryBlock {
    var valEsp = "?,".repeat(valuesSize)
    valEsp = valEsp.substring(0, valEsp.length - 1)
    return QueryBlock(stmt, "NOT IN", "($valEsp)")
}

fun notInList(stmt: String, values: List<Any>): QueryBlock {
    var valEsp = "?,".repeat(values.size)
    valEsp = valEsp.substring(0, valEsp.length - 1)
    return QueryBlock(stmt, "NOT IN", "($valEsp)")
}

fun sub(value: QueryBlock) : QueryBlock {
    return QueryBlock("( ", value.toString(), " )")
}
