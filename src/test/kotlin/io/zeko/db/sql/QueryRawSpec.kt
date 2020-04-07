package io.zeko.db.sql

import io.zeko.db.sql.operators.sub
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals

class QueryRawSpec : Spek({

    fun debug(msg: Any) {
        if (false) println(msg.toString())
    }

    describe("Query Builder raw string statement test") {
        context("Given one table select statement") {

            it("should match sql with fields specified") {
                val sql = Query().fields("id AS user_id", "name AS full_name", "age").from("user").toSql()
                debug(sql)
                assertEquals("SELECT id AS user_id, name AS full_name, age FROM user", sql)
            }

            it("should match sql with fields and where condition") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where("name = 'Bat Man'")
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name = 'Bat Man'", sql)
            }

            it("should match sql with fields and multiple where conditions") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(
                            "name = 'Bat Man'",
                            "id > 1",
                            sub("name LIKE '%bat' OR name LIKE 'man%'")
                        )
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name = 'Bat Man' AND id > 1 AND ( name LIKE '%bat' OR name LIKE 'man%' )", sql)
            }

            it("should match sql with sub query") {
                val sql = Query().fields("id", "name", "age")
                        .from(
                            Query().fields("*").from("user").where("age < 50").limit(10, 0)
                        )
                        .where("name LIKE '%Bat Man'")
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM (SELECT * FROM user WHERE age < 50 LIMIT 10 OFFSET 0) WHERE name LIKE '%Bat Man'", sql)
            }

            it("should match sql with between where conditions of numbers") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(
                            "name = ?",
                            "age BETWEEN 10 AND 50"
                        )
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name = ? AND age BETWEEN 10 AND 50", sql)
            }

            it("should match sql with between where conditions of strings") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(
                    "name BETWEEN ? AND ?"
                        )
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name BETWEEN ? AND ?", sql)
            }
        }
    }
})
