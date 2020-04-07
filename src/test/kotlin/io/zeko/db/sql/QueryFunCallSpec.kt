package io.zeko.db.sql

import io.zeko.db.sql.dsl.*
import io.zeko.db.sql.operators.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals

class QueryFunCallSpec : Spek({

    fun debug(msg: Any) {
        if (false) println(msg.toString())
    }

    describe("Query Builder Function Call test") {
        context("Given one table select statement") {

            it("should match sql with fields and where condition") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(eq("name","Bat Man"))
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name = ?", sql)
            }

            it("should match sql with where condition field value IN a list") {
                val userIds = listOf(1, 12, 18, 25, 55)
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(inList("id",  userIds))
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE id IN (?,?,?,?,?)", sql)
            }

            it("should match sql with where condition field value IN a string list") {
                val names = listOf("Leng", "Bat's Man")
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(inList("name",  names))
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name IN (?,?)", sql)
            }

            it("should match sql with where condition field value IN a list(non-parameterized)") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(inList("id", listOf(1, 12, 18, 25, 55), true))
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE id IN (1,12,18,25,55)", sql)
            }

            it("should match sql with where condition field value IN a string list (non-parameterized)") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(inList("name",  listOf("Leng", "Bat's Man"), true))
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name IN ('Leng','Bat''s Man')", sql)
            }

            it("should match sql with fields and multiple where conditions") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(
                            eq("name", "Bat Man"),
                            greater("id", 1),
                            sub(like("name",  "%bat") or like("name",  "man%")),
                            isNotNull("nickname")
                        )
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name = ? AND id > 1 AND ( name LIKE ? OR name LIKE ? ) AND nickname IS NOT NULL", sql)
            }

            it("should match sql multiple where conditions with raw string values") {
                val sql = Query().fields("id", "name", "age", "nickname")
                        .from("user")
                        .where(
                            eq("name", "nickname", true),
                            greater("id",  1),
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
                            Query().fields("*").from("user").where(less("age", 50)).limit(10, 0)
                        )
                        .where(like("name",  "Bat Man"))
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM (SELECT * FROM user WHERE age < 50 LIMIT 10 OFFSET 0) WHERE name LIKE ?", sql)
            }

            it("should match sql with between where conditions of numbers") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(
                            eq("name", "Bat Man"),
                            between("age", 10, 50)
                        )
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name = ? AND age BETWEEN 10 AND 50", sql)
            }

            it("should match sql with between where conditions of strings") {
                val sql = Query().fields("id", "name", "age")
                        .from("user")
                        .where(
                            between("name", "a", "z")
                        )
                        .toSql()
                debug(sql)
                assertEquals("SELECT id, name, age FROM user WHERE name BETWEEN ? AND ?", sql)
            }

            it("should match sql with fulltext search(MySQL)") {
                val sql = Query().fields("*").from("user")
                        .where(match("name",  "smit"))
                        .toSql()
                debug(sql)
                assertEquals("SELECT * FROM user WHERE MATCH( name ) AGAINST ( ? IN NATURAL LANGUAGE MODE )", sql)
            }

            it("should match sql with fulltext search(MySQL) two columns") {
                val sql = Query().fields("*").from("user")
                        .where(match(listOf("name", "nickname"),  "smit"))
                        .toSql()
                debug(sql)
                assertEquals("SELECT * FROM user WHERE MATCH( name,nickname ) AGAINST ( ? IN NATURAL LANGUAGE MODE )", sql)
            }
        }
    }
})
