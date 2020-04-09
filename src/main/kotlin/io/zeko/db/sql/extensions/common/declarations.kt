package io.zeko.db.sql.extensions.common

import io.zeko.db.sql.CustomPart
import io.zeko.db.sql.Query
import io.zeko.db.sql.QueryBlock


fun Query.forUpdate(): Query {
    this.addExpressionAfter(CustomPart.LIMIT, QueryBlock("FOR UPDATE"))
    return this
}

fun Query.distinct(): Query {
    this.addExpressionAfter(CustomPart.SELECT, QueryBlock("DISTINCT"))
    return this
}

fun Query.union(query: Query): Query {
    this.addExpressionAfter(CustomPart.WHERE, QueryBlock("UNION (", query.toSql(), ")"))
    return this
}

fun Query.unionAll(query: Query): Query {
    this.addExpressionAfter(CustomPart.WHERE, QueryBlock("UNION ALL (", query.toSql(), ")"))
    return this
}
