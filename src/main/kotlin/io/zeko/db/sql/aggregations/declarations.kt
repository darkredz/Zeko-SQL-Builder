package io.zeko.db.sql.aggregations

import io.zeko.db.sql.QueryBlock


fun sum(field: String): QueryBlock {
    return QueryBlock("SUM( ", field, " ) ")
}

fun count(field: String): QueryBlock {
    return QueryBlock("COUNT( ", field, " ) ")
}

fun avg(field: String): QueryBlock {
    return QueryBlock("AVG( ", field, " ) ")
}

fun min(field: String): QueryBlock {
    return QueryBlock("MIN( ", field, " ) ")
}

fun max(field: String): QueryBlock {
    return QueryBlock("MAX( ", field, " ) ")
}

fun sum(field: String, value: Int): QueryBlock {
    return sumEq(field, value)
}

fun sumEq(field: String, value: Int): QueryBlock {
    return agg("SUM", field, "=", value)
}

fun sumGt(field: String, value: Int): QueryBlock {
    return agg("SUM", field, ">", value)
}

fun sumLt(field: String, value: Int): QueryBlock {
    return agg("SUM", field, "<", value)
}

fun sumGe(field: String, value: Int): QueryBlock {
    return agg("SUM", field, ">=", value)
}

fun sumLe(field: String, value: Int): QueryBlock {
    return agg("SUM", field, "<=", value)
}

fun count(field: String, value: Int): QueryBlock {
    return countEq(field, value)
}

fun countEq(field: String, value: Int): QueryBlock {
    return agg("COUNT", field, "=", value)
}

fun countGt(field: String, value: Int): QueryBlock {
    return agg("COUNT", field, ">", value)
}

fun countLt(field: String, value: Int): QueryBlock {
    return agg("COUNT", field, "<", value)
}

fun countGe(field: String, value: Int): QueryBlock {
    return agg("COUNT", field, ">=", value)
}

fun countLe(field: String, value: Int): QueryBlock {
    return agg("COUNT", field, "<=", value)
}

fun avg(field: String, value: Int): QueryBlock {
    return avgEq(field, value)
}

fun avgEq(field: String, value: Int): QueryBlock {
    return agg("AVG", field, "=", value)
}

fun avgGt(field: String, value: Int): QueryBlock {
    return agg("AVG", field, ">", value)
}

fun avgLt(field: String, value: Int): QueryBlock {
    return agg("AVG", field, "<", value)
}

fun avgGe(field: String, value: Int): QueryBlock {
    return agg("AVG", field, ">=", value)
}

fun avgLe(field: String, value: Int): QueryBlock {
    return agg("AVG", field, "<=", value)
}

fun min(field: String, value: Int): QueryBlock {
    return minEq(field, value)
}

fun minEq(field: String, value: Int): QueryBlock {
    return agg("MIN", field, "=", value)
}

fun minGt(field: String, value: Int): QueryBlock {
    return agg("MIN", field, ">", value)
}

fun minLt(field: String, value: Int): QueryBlock {
    return agg("MIN", field, "<", value)
}

fun minGe(field: String, value: Int): QueryBlock {
    return agg("MIN", field, ">=", value)
}

fun minLe(field: String, value: Int): QueryBlock {
    return agg("MIN", field, "<=", value)
}

fun max(field: String, value: Int): QueryBlock {
    return maxEq(field, value)
}

fun maxEq(field: String, value: Int): QueryBlock {
    return agg("MAX", field, "=", value)
}

fun maxGt(field: String, value: Int): QueryBlock {
    return agg("MAX", field, ">", value)
}

fun maxLt(field: String, value: Int): QueryBlock {
    return agg("MAX", field, "<", value)
}

fun maxGe(field: String, value: Int): QueryBlock {
    return agg("MAX", field, ">=", value)
}

fun maxLe(field: String, value: Int): QueryBlock {
    return agg("MAX", field, "<=", value)
}

fun agg(funcName: String, field: String, operator: String, value: Int): QueryBlock {
    return QueryBlock("$funcName(", field, ") $operator $value")
}


fun sumEq(field: String, value: Long): QueryBlock {
    return agg("SUM", field, "=", value)
}

fun sumGt(field: String, value: Long): QueryBlock {
    return agg("SUM", field, ">", value)
}

fun sumLt(field: String, value: Long): QueryBlock {
    return agg("SUM", field, "<", value)
}

fun sumGe(field: String, value: Long): QueryBlock {
    return agg("SUM", field, ">=", value)
}

fun sumLe(field: String, value: Long): QueryBlock {
    return agg("SUM", field, "<=", value)
}

fun count(field: String, value: Long): QueryBlock {
    return countEq(field, value)
}

fun countEq(field: String, value: Long): QueryBlock {
    return agg("COUNT", field, "=", value)
}

fun countGt(field: String, value: Long): QueryBlock {
    return agg("COUNT", field, ">", value)
}

fun countLt(field: String, value: Long): QueryBlock {
    return agg("COUNT", field, "<", value)
}

fun countGe(field: String, value: Long): QueryBlock {
    return agg("COUNT", field, ">=", value)
}

fun countLe(field: String, value: Long): QueryBlock {
    return agg("COUNT", field, "<=", value)
}

fun avg(field: String, value: Long): QueryBlock {
    return avgEq(field, value)
}

fun avgEq(field: String, value: Long): QueryBlock {
    return agg("AVG", field, "=", value)
}

fun avgGt(field: String, value: Long): QueryBlock {
    return agg("AVG", field, ">", value)
}

fun avgLt(field: String, value: Long): QueryBlock {
    return agg("AVG", field, "<", value)
}

fun avgGe(field: String, value: Long): QueryBlock {
    return agg("AVG", field, ">=", value)
}

fun avgLe(field: String, value: Long): QueryBlock {
    return agg("AVG", field, "<=", value)
}

fun min(field: String, value: Long): QueryBlock {
    return minEq(field, value)
}

fun minEq(field: String, value: Long): QueryBlock {
    return agg("MIN", field, "=", value)
}

fun minGt(field: String, value: Long): QueryBlock {
    return agg("MIN", field, ">", value)
}

fun minLt(field: String, value: Long): QueryBlock {
    return agg("MIN", field, "<", value)
}

fun minGe(field: String, value: Long): QueryBlock {
    return agg("MIN", field, ">=", value)
}

fun minLe(field: String, value: Long): QueryBlock {
    return agg("MIN", field, "<=", value)
}

fun max(field: String, value: Long): QueryBlock {
    return maxEq(field, value)
}

fun maxEq(field: String, value: Long): QueryBlock {
    return agg("MAX", field, "=", value)
}

fun maxGt(field: String, value: Long): QueryBlock {
    return agg("MAX", field, ">", value)
}

fun maxLt(field: String, value: Long): QueryBlock {
    return agg("MAX", field, "<", value)
}

fun maxGe(field: String, value: Long): QueryBlock {
    return agg("MAX", field, ">=", value)
}

fun maxLe(field: String, value: Long): QueryBlock {
    return agg("MAX", field, "<=", value)
}

fun agg(funcName: String, field: String, operator: String, value: Long): QueryBlock {
    return QueryBlock("$funcName(", field, ") $operator $value")
}

fun sumEq(field: String, value: Double): QueryBlock {
    return agg("SUM", field, "=", value)
}

fun sumGt(field: String, value: Double): QueryBlock {
    return agg("SUM", field, ">", value)
}

fun sumLt(field: String, value: Double): QueryBlock {
    return agg("SUM", field, "<", value)
}

fun sumGe(field: String, value: Double): QueryBlock {
    return agg("SUM", field, ">=", value)
}

fun sumLe(field: String, value: Double): QueryBlock {
    return agg("SUM", field, "<=", value)
}

fun count(field: String, value: Double): QueryBlock {
    return countEq(field, value)
}

fun countEq(field: String, value: Double): QueryBlock {
    return agg("COUNT", field, "=", value)
}

fun countGt(field: String, value: Double): QueryBlock {
    return agg("COUNT", field, ">", value)
}

fun countLt(field: String, value: Double): QueryBlock {
    return agg("COUNT", field, "<", value)
}

fun countGe(field: String, value: Double): QueryBlock {
    return agg("COUNT", field, ">=", value)
}

fun countLe(field: String, value: Double): QueryBlock {
    return agg("COUNT", field, "<=", value)
}

fun avg(field: String, value: Double): QueryBlock {
    return avgEq(field, value)
}

fun avgEq(field: String, value: Double): QueryBlock {
    return agg("AVG", field, "=", value)
}

fun avgGt(field: String, value: Double): QueryBlock {
    return agg("AVG", field, ">", value)
}

fun avgLt(field: String, value: Double): QueryBlock {
    return agg("AVG", field, "<", value)
}

fun avgGe(field: String, value: Double): QueryBlock {
    return agg("AVG", field, ">=", value)
}

fun avgLe(field: String, value: Double): QueryBlock {
    return agg("AVG", field, "<=", value)
}

fun min(field: String, value: Double): QueryBlock {
    return minEq(field, value)
}

fun minEq(field: String, value: Double): QueryBlock {
    return agg("MIN", field, "=", value)
}

fun minGt(field: String, value: Double): QueryBlock {
    return agg("MIN", field, ">", value)
}

fun minLt(field: String, value: Double): QueryBlock {
    return agg("MIN", field, "<", value)
}

fun minGe(field: String, value: Double): QueryBlock {
    return agg("MIN", field, ">=", value)
}

fun minLe(field: String, value: Double): QueryBlock {
    return agg("MIN", field, "<=", value)
}

fun max(field: String, value: Double): QueryBlock {
    return maxEq(field, value)
}

fun maxEq(field: String, value: Double): QueryBlock {
    return agg("MAX", field, "=", value)
}

fun maxGt(field: String, value: Double): QueryBlock {
    return agg("MAX", field, ">", value)
}

fun maxLt(field: String, value: Double): QueryBlock {
    return agg("MAX", field, "<", value)
}

fun maxGe(field: String, value: Double): QueryBlock {
    return agg("MAX", field, ">=", value)
}

fun maxLe(field: String, value: Double): QueryBlock {
    return agg("MAX", field, "<=", value)
}

fun agg(funcName: String, field: String, operator: String, value: Double): QueryBlock {
    return QueryBlock("$funcName(", field, ") $operator $value")
}

fun agg(funcName: String, field: String, operator: String, value: String): QueryBlock {
    return QueryBlock("$funcName(", field, ") $operator $value")
}
