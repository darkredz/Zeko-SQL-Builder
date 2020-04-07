package io.zeko.db.sql

import io.zeko.db.sql.dsl.*
import io.zeko.db.sql.operators.eq
import io.zeko.db.sql.operators.like
import io.zeko.db.sql.operators.sub
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals

class QuerySpec : Spek({

    fun debug(msg: Any) {
        if (false) println(msg.toString())
    }

    describe("Query Builder DSL test") {
        context("Given one table select statement") {
            it("should match sql with select *") {
                val sql = Query().fields("*").from("user").limit(10, 20).toSql()
                debug(sql)
                assertEquals("SELECT * FROM user LIMIT 10 OFFSET 20", sql)
            }

            it("should match sql with fields specified") {
                val sql = Query().fields("id", "name", "age").from("user").toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user", sql)
            }

            it("should match sql with fields specified and order by") {
                val sql = Query().fields("id", "name", "age").
                        from("user")
                        .order("name")
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user ORDER BY name ASC", sql)
            }

            it("should match sql with fields specified and order by two fields") {
                val sql = Query().fields("id", "name", "age").
                        from("user")
                        .order("id", "name")
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user ORDER BY id ASC, name ASC", sql)
            }

            it("should match sql with fields specified and order by two fields different directions") {
                val sql = Query().fields("id", "name", "age").
                        from("user")
                        .order(Desc("id"), Asc("name"))
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user ORDER BY id DESC, name ASC", sql)
            }

            it("should match sql with fields and where condition") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where("name" eq "Bat Man")
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name = ?", sql)
            }

            it("should match sql with where condition field value IN a list") {
                val userIds = listOf(1, 12, 18, 25, 55)
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where("id" inList userIds)
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE id IN (?,?,?,?,?)", sql)
            }

            it("should match sql with where condition field value IN a string list") {
                val names = listOf("Leng", "Bat's Man")
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where("name" inList names)
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name IN (?,?)", sql)
            }

            it("should match sql with where condition field value IN a list(non-parameterized)") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where("id" inList arrayOf(1, 12, 18, 25, 55))
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE id IN (1,12,18,25,55)", sql)
            }

            it("should match sql with where condition field value IN a string list (non-parameterized)") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where("name" inList arrayOf("Leng", "Bat's Man"))
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name IN ('Leng','Bat''s Man')", sql)
            }

            it("should match sql with fields and multiple where conditions") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(
                    "name" eq "Bat Man" and
                            ("id" greater 1) and
                            sub(("name" like "%bat") or ("name" like "man%")) and
                            ("nickname" isNotNull true)
                        )
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name = ? AND id > 1 AND ( name LIKE ? OR name LIKE ? ) AND nickname IS NOT NULL", sql)
            }

            it("should match sql with fields and multiple where conditions") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(
                                "name" eq "Bat Man" and
                                        ("id" greater 1) and
                                        sub(("name" like "%bat") or ("name" like "man%"))
                        )
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name = ? AND id > 1 AND ( name LIKE ? OR name LIKE ? )", sql)
            }

            it("should match sql multiple where conditions with raw string values") {
                val sql = Query().fields("id", "name", "age", "nickname")
                        .from("user")
                        .where(
                            eq("name", "nickname", true),
                            ("id" greater 1),
                            sub(
                            like("name", "%bat", true) or
                                like("name", "man%", true)
                            )
                        )
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age, nickname FROM user WHERE name = nickname AND id > 1 AND ( name LIKE '%bat' OR name LIKE 'man%' )", sql)
            }

            it("should match sql with sub query") {
                val sql = Query().fields("id", "name", "age")
                        .from(
                                Query().fields("*").from("user").where("age" less 50).limit(10, 0)
                        )
                        .where("name" like "Bat Man")
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM (SELECT * FROM user WHERE age < 50 LIMIT 10 OFFSET 0) WHERE name LIKE ?", sql)
            }

            it("should match sql with between where conditions of numbers") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(
                    "name" eq "Bat Man" and
                            ("age" between (10 to 50))
                        )
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name = ? AND age BETWEEN 10 AND 50", sql)
            }

            it("should match sql with between where conditions of strings") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(
                            ("name" between ("a" to "z"))
                        )
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name BETWEEN ? AND ?", sql)
            }

            it("should match sql with fulltext search(MySQL)") {
                val sql = Query().fields("*").from("user")
                        .where("name" match "smit")
                        .toSql()
                debug(sql)
                assertEquals("SELECT * FROM user WHERE MATCH( name ) AGAINST ( ? IN NATURAL LANGUAGE MODE )", sql)
            }

            it("should match sql with fulltext search(MySQL) two columns") {
                val sql = Query().fields("*").from("user")
                        .where(listOf("name", "nickname") match "smit")
                        .toSql()
                debug(sql)
                assertEquals("SELECT * FROM user WHERE MATCH( name,nickname ) AGAINST ( ? IN NATURAL LANGUAGE MODE )", sql)
            }
        }
    }
})
