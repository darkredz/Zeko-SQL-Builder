package io.zeko.db.sql

import io.zeko.db.sql.dsl.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals

class QueryCustomExpressionSpec : Spek({

    fun debug(msg: Any) {
        if (true) println(msg.toString())
    }

    describe("Query Builder Custom Expression test") {
        context("Given a query extension forUpdate()") {
            it("should match sql with Select...For Update") {
                fun Query.forUpdate(): Query {
                    this.addExpressionAfter(CustomPart.LIMIT, QueryBlock("FOR UPDATE"))
                    return this
                }
                val sql = Query().fields("*").from("user")
                        .where("name" eq "Leng")
                        .limit(1).forUpdate()
                        .toSql()
                debug(sql)
                assertEquals("SELECT * FROM user WHERE name = ? LIMIT 1 OFFSET 0 FOR UPDATE", sql)
            }
        }

        context("Given a query extension union()") {
            it("should match sql with Select...Union Select...") {
                fun Query.union(query: Query): Query {
                    this.addExpressionAfter(CustomPart.WHERE, QueryBlock("UNION (", query.toSql(), ")"))
                    return this
                }

                val sql = Query().fields("id", "name").from("user")
                        .where("name" eq "Leng")
                        .union(
                            Query().fields("id", "first_name").from("customer")
                        )
                        .order("first_name")
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name FROM user WHERE name = ? UNION ( SELECT id, first_name FROM customer ) ORDER BY first_name ASC", sql)
            }
        }

        context("Given multiple query extension") {
            it("should match sql with Select Distinct...Union Select...") {
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

                val sql = Query().fields("id", "last_name").from("user")
                        .where("name" eq "Leng")
                        .union(
                            Query().distinct().fields("id", "first_name").from("customer")
                        )
                        .unionAll(
                            Query().fields("id", "first_name").from("customer_blacklist").where("status" eq 1)
                        )
                        .order("user.id")
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, last_name FROM user WHERE name = ? " +
                        "UNION ( SELECT DISTINCT id, first_name FROM customer ) " +
                        "UNION ALL ( SELECT id, first_name FROM customer_blacklist " +
                        "WHERE status = 1 ) ORDER BY user.id ASC", sql)
            }
        }
    }
})
