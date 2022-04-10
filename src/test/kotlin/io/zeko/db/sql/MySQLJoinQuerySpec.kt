package io.zeko.db.sql

import io.zeko.db.sql.aggregations.*
import io.zeko.db.sql.dsl.*
import io.zeko.db.sql.operators.isNotNull
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals

class MySQLJoinQuerySpec : Spek({

    fun debug(msg: Any) {
        if (false) println(msg.toString())
    }

    describe("Query Builder DSL Join Queries MySQL test") {
        context("Given multiple tables join(user has address, and has many roles)") {
            it("should match sql with one table left join") {
                val sql = Query().fields("*")
                        .from("user")
                        .leftJoin("address").on("user_id = user.id")
                        .toSql()
                debug(sql)
                assertEquals("SELECT * FROM user LEFT JOIN address ON (address.user_id = user.id )", sql)
            }

            it("should match sql with one table inner join with field rename") {
                val sql = Query().fields("user.id AS user_id", "name", "street1", "street2", "address.id AS address_id")
                        .from("user")
                        .innerJoin("address").on("user_id = user.id")
                        .toSql()
                debug(sql)
                assertEquals("SELECT user.id AS user_id, name, street1, street2, address.id AS address_id FROM user INNER JOIN address ON (address.user_id = user.id )", sql)
            }

            it("should match sql with one table left join with field selection and where conditions") {
                val sql = Query()
                        .table("user").fields("id", "name")
                        .table("address").fields("id", "street1", "street2", "user.id = user_id")
                        .from("user")
                        .leftJoin("address").on("user_id = user.id")
                        .where(
                "user.status" greater 0 or
                            isNotNull("user.id")
                        )
                        .orderDesc("user.name")
                        .toSql()
                debug(sql)
                assertEquals("SELECT user.id as `user-id`, user.name as `user-name`, address.id as `address-id`, address.street1 as `address-street1`, address.street2 as `address-street2`, user.id as `address-user_id` FROM user LEFT JOIN address ON (address.user_id = user.id ) WHERE user.status > 0 OR user.id IS NOT NULL ORDER BY user.name DESC", sql)
            }

            it("should match sql with three table joins") {
                val sql = Query()
                        .table("user").fields("id", "name")
                        .table("role").fields("id", "role_name", "user.id = user_id")
                        .table("address").fields("id", "street1", "street2", "user.id = user_id")
                        .from("user")
                        .leftJoin("address").on("user_id = user.id")
                        .leftJoin("user_has_role").on("user_id = user.id")
                        .leftJoin("role").on("id = user_has_role.role_id")
                        .where(
                    "user.status" greater 0 or
                            ("user.id" notInList arrayOf(1, 2, 3))
                        )
                        .order("user.id")
                        .toSql()
                debug(sql)
                assertEquals("SELECT user.id as `user-id`, user.name as `user-name`, role.id as `role-id`, role.role_name as `role-role_name`, user.id as `role-user_id`, address.id as `address-id`, address.street1 as `address-street1`, address.street2 as `address-street2`, user.id as `address-user_id` FROM user LEFT JOIN address ON (address.user_id = user.id ) LEFT JOIN user_has_role ON (user_has_role.user_id = user.id ) LEFT JOIN role ON (role.id = user_has_role.role_id ) WHERE user.status > 0 OR user.id NOT IN (1,2,3) ORDER BY user.id ASC", sql)
            }

            it("should match sql with three table joins with select from subquery") {
                val sql = Query()
                        .table("user").fields("id", "name")
                        .table("role").fields("id", "role_name", "user.id = user_id")
                        .table("address").fields("id", "street1", "street2", "user.id = user_id")
                        .from(
                            Query().fields("*").from("user")
                                    .where("id" lessEq 100 and ("age" greater 50))
                                    .order("user.id")
                                    .limit(10, 0)
                                    .asTable("user")
                        )
                        .leftJoin("address").on("user_id = user.id")
                        .leftJoin("user_has_role").on("user_id = user.id")
                        .leftJoin("role").on("id = user_has_role.role_id")
                        .where(
                    "user.status" greater 0 or
                            ("user.id" notInList arrayOf(1, 2, 3))
                        )
                        .order("user.name")
                        .toSql()
                debug(sql)
                assertEquals("SELECT user.id as `user-id`, user.name as `user-name`, role.id as `role-id`, " +
                        "role.role_name as `role-role_name`, user.id as `role-user_id`, address.id as `address-id`, " +
                        "address.street1 as `address-street1`, address.street2 as `address-street2`, user.id as " +
                        "`address-user_id` FROM (SELECT * FROM user WHERE id <= 100 AND age > 50 " +
                        "ORDER BY user.id ASC LIMIT 10 OFFSET 0) AS user LEFT JOIN address ON (address.user_id = user.id ) " +
                        "LEFT JOIN user_has_role ON (user_has_role.user_id = user.id ) " +
                        "LEFT JOIN role ON (role.id = user_has_role.role_id ) " +
                        "WHERE user.status > 0 OR user.id NOT IN (1,2,3) " +
                        "ORDER BY user.name ASC", sql)
            }

            it("should match sql with three table joins with group by and having") {
                val sql = Query()
                        .table("user").fields("id", "name")
                        .table("role").fields("id", "role_name", "user.id = user_id")
                        .table("address").fields("id", "street1", "street2", "user.id = user_id")
                        .from("user")
                        .leftJoin("address").on("user_id = user.id")
                        .leftJoin("user_has_role").on("user_id = user.id")
                        .leftJoin("role").on("id = user_has_role.role_id")
                        .where(
                    "user.status" greater 0 or
                            ("user.id" notInList arrayOf(1, 2, 3))
                        )
                        .groupBy("role.id", "role.name")
                        .having(
                            sumGt("role.id", 2),
                            count("role.id") less 10
                        )
                        .order("user.id")
                        .limit(10, 20)
                        .toSql()
                debug(sql)
                assertEquals("SELECT user.id as `user-id`, user.name as `user-name`, role.id as `role-id`, " +
                        "role.role_name as `role-role_name`, user.id as `role-user_id`, address.id as `address-id`, " +
                        "address.street1 as `address-street1`, address.street2 as `address-street2`, user.id as " +
                        "`address-user_id` FROM user LEFT JOIN address ON (address.user_id = user.id ) " +
                        "LEFT JOIN user_has_role ON (user_has_role.user_id = user.id ) " +
                        "LEFT JOIN role ON (role.id = user_has_role.role_id ) " +
                        "WHERE user.status > 0 OR user.id NOT IN (1,2,3) " +
                        "GROUP BY role.id, role.name " +
                        "HAVING SUM( role.id ) > 2 AND COUNT( role.id ) < 10 ORDER BY user.id ASC LIMIT 10 OFFSET 20", sql)
            }

            it("should match sql with one table inner join a subquery") {
                val sql = Query().fields("*")
                    .from("user")
                    .innerJoin(
                        Query().fields("id", "user_id", "(total_savings - total_spendings) as balance").from("report"),
                        "user_wallet"
                    )
                    .on("user_wallet.user_id = user.id")
                    .toSql()
                debug(sql)
                assertEquals("""
                    SELECT * FROM user INNER JOIN ( 
                    SELECT id, user_id, (total_savings - total_spendings) as balance FROM report ) as user_wallet 
                    ON ( user_wallet.user_id = user.id )
                """.trimIndent().replace("\n", ""), sql)
            }
        }
    }
})
