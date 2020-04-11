package io.zeko.db.sql

import io.zeko.db.sql.dsl.*
import io.zeko.model.entities.Address
import io.zeko.model.entities.User
import io.zeko.model.entities.UserRole
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals

class DeleteStatementSpec : Spek({

    fun debug(msg: Any) {
        if (false) println(msg.toString())
    }

    describe("Query Builder Delete Statement test") {
        context("Given a set of Entities User, UserRole and Address") {

            it("should match delete sql when passing entity with properties set") {
                val sql = Delete(User().apply {
                    name = "O'Connor"
                    id = 111
                }).toSql()
                debug(sql)
                assertEquals("DELETE FROM users WHERE name = 'O''Connor' AND id = 111", sql)
            }

            it("should match delete parameterized sql when passing entity with properties set") {
                val user = User().apply {
                    name = "O'Connor"
                    id = 111
                }
                val sql = Delete(user, true).toSql()
                debug(sql)
                assertEquals("DELETE FROM users WHERE name = ? AND id = ?", sql)
            }

            it("should match delete sql using where conditions") {
                val sql = Delete(User()).where(
                            "id" greater 100,
                            "age" less 80,
                            "name" eq "Leng"
                        ).toSql()
                debug(sql)
                assertEquals("DELETE FROM users WHERE id > 100 AND age < 80 AND name = ?", sql)
            }

            it("should match delete sql using where conditions and escaped table name") {
                val sql = Delete(User()).escapeTable(true).where(
                            "id" greater 100,
                            "age" less 80,
                            "name" eq "Leng"
                        ).toSql()
                debug(sql)
                assertEquals("DELETE FROM \"users\" WHERE id > 100 AND age < 80 AND name = ?", sql)
            }

            it("should match delete sql using where by passing Query object") {
                val sql = Delete(User()).where(
                            Query().where(
                                "id" greater 100,
                                "name" eq "Leng"
                            )
                        ).toSql()
                debug(sql)
                assertEquals("DELETE FROM users WHERE id > 100 AND name = ?", sql)
            }

            it("should match delete sql using where Query if given entity properties & Query at the same time") {
                val sql = Delete(User().apply {
                    name = "John"
                    id = 111
                }).where(
                    Query().where("id" greater 100)
                ).toSql()
                debug(sql)
                assertEquals("DELETE FROM users WHERE id > 100", sql)
            }

            it("should match delete sql using primitive typed properties only") {
                val user = User().apply {
                    age = 60
                    id = 12
                    roles = listOf(UserRole().apply { role_name = "admin" })
                    addresses = listOf(Address().apply { street1 = "jalan 123" })
                }
                val sql = Delete(user).toSql()
                debug(sql)
                assertEquals("DELETE FROM users WHERE age = 60 AND id = 12", sql)
            }

            it("should match delete sql when using entity class name as table") {
                val sql = Delete(Address().apply {
                    user_id = 1
                }).toSql()
                debug(sql)
                assertEquals("DELETE FROM address WHERE user_id = 1", sql)
            }
        }
    }
})
