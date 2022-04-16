package io.zeko.db.sql

import io.zeko.db.sql.dsl.*
import io.zeko.model.entities.Address
import io.zeko.model.entities.User
import io.zeko.model.entities.UserRole
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals

class InsertStatementSpec : Spek({

    fun debug(msg: Any) {
        if (true) println(msg.toString())
    }

    describe("Query Builder Insert Statement test") {
        context("Given a set of Entities User, UserRole and Address") {

            it("should match insert sql when passing entity with properties set with Map") {
                val sql = Insert(User(
                            mapOf(
                                "id" to 1,
                                "name" to "O'Connor"
                            )
                        )).toSql()
                debug(sql)
                assertEquals("INSERT INTO users ( id, name ) VALUES ( 1, 'O''Connor' )", sql)
            }

            it("should match insert parameterized sql when passing entity with properties set with Map") {
                val sql = Insert(User(
                            mapOf(
                                "id" to 1,
                                "name" to "O'Connor"
                            )
                        ), true).toSql()
                debug(sql)
                assertEquals("INSERT INTO users ( id, name ) VALUES ( ?, ? )", sql)
            }

            it("should match insert sql when passing pairs") {
                val sql = Insert(User(
                            "id" to 1,
                            "name" to "O'Connor"
                        )).toSql()
                debug(sql)
                assertEquals("INSERT INTO users ( id, name ) VALUES ( 1, 'O''Connor' )", sql)
            }

            it("should match insert sql for column part only when not passing in any properties") {
                val sql = Insert(User(), "id", "name").toSql()
                debug(sql)
                assertEquals("INSERT INTO users ( id, name )", sql)
            }

            it("should match insert...select sql when passing Query object") {
                val sql = Insert(User(), "id", "name").select(
                            Query().fields("user_id", "fullname")
                                    .from("customer")
                                    .where("status" eq 0)
                        ).toSql()
                debug(sql)
                assertEquals("INSERT INTO users ( id, name ) SELECT user_id, fullname FROM customer WHERE status = 0", sql)
            }

            it("should match insert sql when using escape table name") {
                val sql = Insert(User(
                            "id" to 1,
                            "name" to "Leng"
                        ))
                        .escapeTable(true)
                        .toSql()
                debug(sql)
                assertEquals("INSERT INTO \"users\" ( id, name ) VALUES ( 1, 'Leng' )", sql)
            }

            it("should match insert sql when using entity class name as table") {
                val sql = Insert(Address().apply {
                            user_id = 1
                            street1 = "Jalan 123"
                            street2 = "Taman OUG"
                        }).toSql()
                debug(sql)
                assertEquals("INSERT INTO address ( user_id, street1, street2 ) VALUES ( 1, 'Jalan 123', 'Taman OUG' )", sql)
            }

            it("should match insert sql using primitive typed properties only") {
                val user = User().apply {
                    age = 60
                    id = 12
                    roles = listOf(UserRole().apply { role_name = "admin" })
                    addresses = listOf(Address().apply { street1 = "jalan 123" })
                }
                val sql = Insert(user).toSql()
                debug(sql)
                assertEquals("INSERT INTO users ( age, id ) VALUES ( 60, 12 )", sql)
            }

            it("should match insert...select and on duplicate key update sql when passing Query object") {
                val sql = Insert(User(), "id", "name").select(
                    Query().fields("user_id", "fullname")
                        .from("customer")
                        .where("status" eq 0)
                ).onDuplicateUpdate(hashMapOf(
                    "repeated" to QueryBlock("repeated", "+", "1"),
                    "is_modified" to true
                ))
                    .toSql()
                debug(sql)
                assertEquals("INSERT INTO users ( id, name ) SELECT user_id, fullname FROM customer WHERE status = 0 ON DUPLICATE KEY UPDATE repeated = repeated + 1, is_modified = true", sql)
            }

            it("should match insert...on duplicate key update sql when passing Query object") {
                val user = User().apply {
                    id = 3
                    name = "Joey Tan"
                    age = 43
                }
                val sql = Insert(user).onDuplicateUpdate(hashMapOf(
                    "repeated" to QueryBlock("repeated", "+", "1"),
                    "is_modified" to true
                )).toSql()
                debug(sql)
                assertEquals("INSERT INTO users ( id, name, age ) VALUES ( 3, 'Joey Tan', 43 )  ON DUPLICATE KEY UPDATE repeated = repeated + 1, is_modified = true", sql)
            }

            it("should match insert...on duplicate key update parameterized sql when passing Query object") {
                val user = User().apply {
                    id = 3
                    name = "Joey Tan"
                    age = 43
                }
                val sql = Insert(user, true).onDuplicateUpdate(hashMapOf(
                    "repeated" to QueryBlock("repeated", "+", "?"),
                    "is_modified" to true
                )).toSql()
                debug(sql)
                assertEquals("INSERT INTO users ( id, name, age ) VALUES ( ?, ?, ? )  ON DUPLICATE KEY UPDATE repeated = repeated + ?, is_modified = ?", sql)
            }

            it("should match insert ignore sql when ignore() is used") {
                val sql = Insert(User(
                    "id" to 1,
                    "name" to "Leng"
                ))
                    .ignore()
                    .toSql()
                debug(sql)
                assertEquals("INSERT IGNORE INTO users ( id, name ) VALUES ( 1, 'Leng' )", sql)
            }
        }
    }
})
