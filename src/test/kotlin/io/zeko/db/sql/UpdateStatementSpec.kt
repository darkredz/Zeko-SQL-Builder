package io.zeko.db.sql

import io.zeko.db.sql.dsl.*
import io.zeko.model.entities.Address
import io.zeko.model.entities.User
import io.zeko.model.entities.UserRole
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals

class UpdateStatementSpec : Spek({

    fun debug(msg: Any) {
        if (false) println(msg.toString())
    }

    describe("Query Builder Update Statement test") {
        context("Given a set of Entities User, UserRole and Address") {

            it("should match update sql when passing entity with properties set with Map") {
                val sql = Update(User(
                            mapOf(
                                "id" to 1,
                                "name" to "O'Connor"
                            )
                        )).toSql()
                debug(sql)
                assertEquals("UPDATE users SET id = 1, name = 'O''Connor'", sql)
            }

            it("should match update parameterized sql when passing entity with properties set with Map") {
                val sql = Update(User(
                            mapOf(
                                "id" to 1,
                                "name" to "O'Connor"
                            )
                        ), true).toSql()
                debug(sql)
                assertEquals("UPDATE users SET id = ?, name = ?", sql)
            }

            it("should match update sql when passing pairs") {
                val sql = Update(User(
                            "id" to 1,
                            "name" to "O'Connor"
                        )).toSql()
                debug(sql)
                assertEquals("UPDATE users SET id = 1, name = 'O''Connor'", sql)
            }

            it("should match update sql using where by passing Query object") {
                val sql = Update(User().apply {
                            name = "Leng"
                        }).where(
                            Query().where(
                                "id" greater 100,
                                "age" eq 60
                            )
                        ).toSql()
                debug(sql)
                assertEquals("UPDATE users SET name = 'Leng' WHERE id > 100 AND age = 60", sql)
            }

            it("should match update sql using where by passing multiple conditions") {
                val sql = Update(User().apply {
                            name = "Leng"
                        }).where(
                            "id" greater 100,
                            "age" eq 60
                        ).toSql()
                debug(sql)
                assertEquals("UPDATE users SET name = 'Leng' WHERE id > 100 AND age = 60", sql)
            }

            it("should match update sql using primitive typed properties only") {
                val user = User().apply {
                    age = 60
                    id = 12
                    roles = listOf(UserRole().apply { role_name = "admin" })
                    addresses = listOf(Address().apply { street1 = "jalan 123" })
                }
                val sql = Update(user).toSql()
                debug(sql)
                assertEquals("UPDATE users SET age = 60, id = 12", sql)
            }

            it("should match update sql when using escape table name") {
                val sql = Update(User(
                            "id" to 1,
                            "name" to "Leng"
                        ))
                        .escapeTable(true)
                        .toSql()
                debug(sql)
                assertEquals("UPDATE \"users\" SET id = 1, name = 'Leng'", sql)
            }

            it("should match update sql when using entity class name as table") {
                val sql = Update(Address().apply {
                    user_id = 1
                    street1 = "Jalan 123"
                    street2 = "Taman OUG"
                }).toSql()
                debug(sql)
                assertEquals("UPDATE address SET user_id = 1, street1 = 'Jalan 123', street2 = 'Taman OUG'", sql)
            }
        }
    }
})
