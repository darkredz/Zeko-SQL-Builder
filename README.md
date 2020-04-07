# Zeko SQL Builder
![alt Zeko SQL Builder](./logo.svg "Zeko lightweight SQL Builder")

Zeko SQL Builder is a high-performance lightweight SQL library written for Kotlin language. It is designed to be flexible, portable, and fun to use. This library provides handy SQL wrapping DSL and a RDB client which is an abstraction on top on JDBC (currently supports HikariCP and Vert.x JDBC driver client)

##Getting Started
This library is very easy-to-use. After reading this short documentation, you will have learnt enough.
There's 3 kinds of flavour.

###SQL Query Builder
The query builder dsl is currently supports standard ANSI sql which had been tested on database dialects
 such as PostgreSQL, MySQL, MariaDB, Apache Ignite and SQLite. 
 
Simplest example, this only generates SQL without any database connection & execution.

####Simple Query 
```kotlin
import io.zeko.db.sql.Query

Query().fields("*").from("user").toSql()
Query().fields("id", "name", "age").from("user").toSql()
```

Outputs:
```sql
SELECT * FROM user
SELECT id, name, age FROM user
```

####Query DSL with where conditions and subquery
```kotlin
import io.zeko.db.sql.Query

Query().fields("id", "name", "age")
    .from("user")
    .where("id" inList arrayOf(1, 12, 18, 25, 55))
    .toSql()

val names = listOf("Leng", "Bat's Man")
Query().fields("id", "name", "age")
    .from("user")
    .where("name" inList names)
    .toSql()

Query().fields("id", "name", "age")
    .from("user")
    .where(
        "name" eq "Bat Man" and
        ("id" greater 1) and
        sub(("name" like "%bat") or ("name" like "man%")) and
        isNotNull("nickname")
    ).toSql()

//Subquery
Query().fields("id", "name", "age")
    .from(
        Query().fields("*").from("user").where("age" less 50).limit(10, 0)
    )
    .where("name" like "Bat Man")
    .toSql()
```

Outputs:
```sql
SELECT id, name, age FROM user WHERE name IN (?,?)
SELECT id, name, age FROM user WHERE id IN (1,12,18,25,55)
SELECT id, name, age FROM user WHERE name = ? AND id > 1 AND ( name LIKE ? OR name LIKE ? ) AND nickname IS NOT NULL
SELECT id, name, age FROM (SELECT * FROM user WHERE age < 50 LIMIT 10 OFFSET 0) WHERE name LIKE ?
```


####Table Joins and aggregation functions
```kotlin
Query().fields("*")
   .from("user")
   .leftJoin("address").on("user_id = user.id")
   .toSql()

// With column alias, group by and having
Query()
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
```

Outputs
```sql
SELECT * FROM user LEFT JOIN address ON (address.user_id = user.id )
SELECT user.id as `user-id`, user.name as `user-name`, role.id as `role-id`, role.role_name as `role-role_name`, user.id as `role-user_id`, address.id as `address-id`, address.street1 as `address-street1`, address.street2 as `address-street2`, user.id as `address-user_id` FROM user LEFT JOIN address ON (address.user_id = user.id ) LEFT JOIN user_has_role ON (user_has_role.user_id = user.id ) LEFT JOIN role ON (role.id = user_has_role.role_id ) WHERE user.status > 0 OR user.id NOT IN (1,2,3) GROUP BY role.id, role.name HAVING SUM( role.id ) > 2 AND COUNT( role.id ) < 10 ORDER BY user.id ASC LIMIT 10 OFFSET 20
```


####MySQL Fulltext search
```kotlin
Query().fields("*").from("user").where("name" match "smit").toSql()
Query().fields("*").from("user")
    .where(listOf("name", "nickname") match "smit")
    .toSql()
```

Outputs
```sql
SELECT * FROM user WHERE MATCH( name ) AGAINST ( ? IN NATURAL LANGUAGE MODE )
SELECT * FROM user WHERE MATCH( name,nickname ) AGAINST ( ? IN NATURAL LANGUAGE MODE )
```

####Where expression
Query expression (where) allowed conditions are:
```
eq - (==)
neq - (!=)
isNull()
isNotNull()
less - (<)
lessEq - (<=)
greater - (>)
greaterEq - (>=)
like - (=~)
notLike - (!~)
regexp
notRegexp
inList
notInList
between
match (MySQL MATCH AGAINST) 
sub - (This is use to group nested conditions)
```

####Aggregation functions
Query aggregation allowed functions are:
```
sum()
count()
avg()
min()
max()

// to compare equal to value
sumEq()
countEq()
avgEq()
minEq()
maxEq()

// to compare greater than value
sumGt()
countGt()
avgGt()
minGt()
maxGt()

// to compare less than value
sumLt()
countLt()
avgLt()
minLt()
maxLt()
```
Or use agg(funcName, field, operator, value) to add in your desired aggregation function

####More Examples
Look at the test cases for more [SQL code samples](https://github.com/darkredz/Zeko-SQL-Builder/tree/dev/src/test/kotlin/io/zeko/db/sql)


####Query Dialects
The Query class is used for MySQL dialect by default. 
To use it with other RDBMS such as Postgres, MariaDB and SQLite, you can use [ANSIQuery](https://github.com/darkredz/Zeko-SQL-Builder/blob/master/src/main/kotlin/io/zeko/db/sql/ANSIQuery.kt) instead of Query class.
Or extend it to set your intended dialect column escape character.

Example: Apache Ignite query class - [IgniteQuery](https://github.com/darkredz/Zeko-SQL-Builder/blob/master/src/main/kotlin/io/zeko/db/sql/IgniteQuery.kt)


##Download 

    <dependency>
      <groupId>io.zeko</groupId>
      <artifactId>zeko-data-mapper</artifactId>
      <version>1.5.10</version>
    </dependency>
    
