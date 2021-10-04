# Zeko SQL Builder
![alt Zeko SQL Builder](./logo.svg "Zeko lightweight SQL Builder")

<p align="left">
    <a href="https://search.maven.org/search?q=g:%22io.zeko%22">
        <img src="https://img.shields.io/maven-central/v/io.zeko/zeko-sql-builder.svg?label=Maven%20Central" alt="Maven Central" />
    </a>
    <a href="LICENSE">
        <img src="https://img.shields.io/badge/license-Apache%202-blue.svg?maxAge=2592000" alt="Apache License 2" />
    </a>
    <a href="https://github.com/KotlinBy/awesome-kotlin">
        <img src="https://kotlin.link/awesome-kotlin.svg" alt="Awesome Kotlin Badge" />
    </a>
</p>

Zeko SQL Builder is a high-performance lightweight SQL library written for Kotlin language. It is designed to be flexible, portable, and fun to use. This library provides handy SQL wrapping DSL and a RDB client which is an abstraction on top on JDBC (currently supports HikariCP and Vert.x JDBC driver client).

This library is open source and available under the Apache 2.0 license. Please leave a star if you've found this library helpful!

## Features
- No configuration files, no XML, no YAML, no annotations, lightweight, easy to use
- Fast startup & performance
- No dependencies ([Jasync SQL](https://github.com/jasync-sql/jasync-sql), [Hikari-CP](https://github.com/brettwooldridge/HikariCP) & [Vert.x JDBC module](https://vertx.io/docs/vertx-jdbc-client/kotlin/) are optional)
- Flexible queries, gain control over the generated SQL as you wish
- No implicit fetching. Just generating SQL through the DSL
- No magic. No proxies, interceptors, reflections
- No overheads, mainly string manipulation
- Extensible, [add your own extensions](#custom-Extensions) to support more operators, database dialects, SQL functions
 
## Getting Started
This library is very easy-to-use. After reading this short documentation, you will have learnt enough.
There's 3 kinds of flavours in writing queries with the library.

## SQL Query Builder
The query builder dsl currently supports standard ANSI sql which had been tested on database dialects
 such as PostgreSQL, MySQL, MariaDB, Apache Ignite and SQLite. 
 

## Examples
Simplest example, this only generates SQL without any database connection & execution.

#### Simple Query 
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

#### Query DSL with where conditions and subquery
```kotlin
import io.zeko.db.sql.Query
import io.zeko.db.sql.dsl.*
import io.zeko.db.sql.operators.*

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


#### Table Joins and aggregation functions
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

#### MySQL Fulltext search
```kotlin
Query().fields("*").from("user").where("name" match "smit").toSql()
Query().fields("*").from("user")
    .where(listOf("name", "nickname") match "smit")
    .toSql()
```

#### Unions
```kotlin
import io.zeko.db.sql.extensions.common.*

Query().fields("id", "name").from("user")
    .where("name" eq "Leng")
    .union(
        Query().fields("id", "first_name").from("customer")
    )
    .order("first_name")
    .toSql()
```

Outputs
```sql
SELECT id, name FROM user WHERE name = ? UNION ( SELECT id, first_name FROM customer ) ORDER BY first_name ASC
```

## Different style of writing query
You can mixed all 3 together if needed.

#### Standard DSL
```kotlin
Query().fields("id", "name", "age")
    .from("user")
    .where(
        "name" eq "Bat Man" and
        ("id" greater 1) and
        sub(("name" like "%bat") or ("name" like "man%")) and
        ("nickname" isNotNull true)
    ).toSql()
```

#### Static function call
```kotlin
Query().fields("id", "name", "age")
    .from("user")
    .where(
        eq("name", "Bat Man"),
        greater("id", 1),
        sub(like("name", "%bat") or like("name", "man%")),
        isNotNull("nickname")
    ).toSql()
```

#### Raw strings
```kotlin
Query().fields("id", "name", "age")
    .from("user")
    .where(
        "name = 'Bat Man'",
        "id > 1",
        "(name LIKE '%bat' OR name LIKE 'man%'",
        "nickname IS NOT NULL"
    ).toSql()
```

## Insert, Update, Merge and Delete statements
Zeko sql builder only supports DML at the moment. To build any insert, update and delete statement, you have to create an Entity class for your table.

```kotlin
import io.zeko.model.Entity

class User : Entity {
    constructor(map: Map<String, Any?>) : super(map)
    constructor(vararg props: Pair<String, Any?>) : super(*props)
    var id: Int?     by map
    var age: Int?     by map
    var name: String? by map
    var roleId: Int? by map
    var role: List<Role>? by map
    var address: List<Address>? by map
}
```
Generate SQL, for more [examples refer to the test cases](https://github.com/darkredz/Zeko-SQL-Builder/blob/master/src/test/kotlin/io/zeko/db/sql/UpdateStatementSpec.kt)
```kotlin
Update(User("id" to 1, "name" to "O'Connor")).toSql()
// UPDATE users SET id = 1, name = 'O''Connor'

Insert(User().apply {
    name = "Leng"
    age = 21
}).toSql()
// INSERT INTO user ( name, age ) VALUES ( 'Leng', 21 )

Update(User().apply {
    name = "Leng"
}).where(
    "id" greater 100,
    "age" eq 60
).toSql()
// UPDATE users SET name = 'Leng' WHERE id > 100 AND age = 60

Insert(User(), "id", "name").select(
    Query().fields("user_id", "fullname")
            .from("customer")
            .where("status" eq 0)
).toSql()
// INSERT INTO users ( id, name ) SELECT user_id, fullname FROM customer WHERE status = 0

 Delete(User()).where(
    "id" greater 100,
    "age" less 80,
    "name" eq "Leng"
).toSql()
// DELETE FROM users WHERE id > 100 AND age < 80 AND name = ?


Merge(User().apply {
    id = 2
    name = "Leng"
}).toSql()
// INSERT INTO user ( id, name ) VALUES ( 2, 'Leng' )
```

#### Parameterize Query for Insert/Update/Merge
Pass true as the second parameter to Insert/Update/Merge will generate SQL with *?*

Use *Entity::toParams()* method to get the list of values to be used with your DB client for a prepared statement DML 

```kotlin
val user = User().apply {
               name = "Leng"
               age = 21
           }
Insert(user, true).toSql()
// INSERT INTO user ( name, age ) VALUES ( ?, ? )

println(user.toParams())
// 'Leng', 21
```

If your entity has properties with custom classes such as Enum, override toParams() method to return the value type needed to store in your RDBMS:
```kotlin
enum class RoleType(val type: Int) {
    ADMIN(1),
    NORMAL_USER(2),
    MODERATOR(3)
}

class User : Entity {
    // ... code as User class above ...
    var roleType: RoleType?     by map
    // Override toParams to convert role type Enum into int 
    // to be stored in the Table role_type column (TinyInt/Int)
    override fun toParams(valueHandler: ((String, Any?) -> Any?)?): List<Any?> {
        return super.toParams { key, value ->
            when (key) {
                "roleType" -> (value as RoleType).type
                else -> value
            }
        }
    }
}

val user = User().apply {
               name = "Leng"
               roleType = RoleType.ADMIN
           }
Insert(user, true).toSql()
// INSERT INTO user ( name, role_type ) VALUES ( ?, ? )

println(user.toParams())
// 'Leng', 1
```

## Where Expression
Query expression (where) allowed conditions are:
```
eq - (=)
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

## Aggregation functions
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

#### Add your own aggregation functions
```kotlin
// Postgres square of the correlation coefficient
fun regr_r2(field: String, value: Double): QueryBlock {
    return agg("regr_r2", field, "=", value)
}

fun regr_r2_gt(field: String, value: Double): QueryBlock {
    return agg("regr_r2", field, ">", value)
}
```

## Custom Extensions
You can add your own custom queries extensions for you desired database dialects. 
It is made simple and possible in Kotlin language. 

For example, you can add in FOR UPDATE statements which is used in MySQL by simply defining a method extension in your package and import to use it.
```kotlin
fun Query.forUpdate(): Query {
    // Adds query block after LIMIT section
    this.addExpressionAfter(CustomPart.LIMIT, QueryBlock("FOR UPDATE"))
    return this
}

Query().fields("*").from("user")
    .where("name" eq "Leng")
    .limit(1)
    .forUpdate()
    .toSql()

// Outputs: SELECT * FROM user WHERE name = ? LIMIT 1 OFFSET 0 FOR UPDATE
```

Available SQL section for customization are defined at [CustomPart enum class](https://github.com/darkredz/Zeko-SQL-Builder/blob/master/src/main/kotlin/io/zeko/db/sql/CustomPart.kt)
```
    SELECT, FIELD, FROM, JOIN, WHERE, GROUP_BY, HAVING, ORDER, LIMIT
```

For more examples such as [UNION](https://github.com/darkredz/Zeko-SQL-Builder/blob/master/src/main/kotlin/io/zeko/db/sql/extensions/common/declarations.kt#L18) refer to the [predefined extensions source code](https://github.com/darkredz/Zeko-SQL-Builder/blob/master/src/main/kotlin/io/zeko/db/sql/extensions/common/declarations.kt)
and the test cases for [custom extensions](https://github.com/darkredz/Zeko-SQL-Builder/blob/master/src/test/kotlin/io/zeko/db/sql/QueryCustomExpressionSpec.kt)

## More Examples
Look at the test cases for more [SQL code samples](https://github.com/darkredz/Zeko-SQL-Builder/tree/dev/src/test/kotlin/io/zeko/db/sql)

## Query Dialects
The Query class is used for MySQL dialect by default. 
To use it with other RDBMS such as Postgres, MariaDB and SQLite, you can use [ANSIQuery](https://github.com/darkredz/Zeko-SQL-Builder/blob/master/src/main/kotlin/io/zeko/db/sql/ANSIQuery.kt) instead of Query class.
Or extend it to set your intended dialect column escape character.

Example: Apache Ignite query class - [IgniteQuery](https://github.com/darkredz/Zeko-SQL-Builder/blob/master/src/main/kotlin/io/zeko/db/sql/IgniteQuery.kt)

## Database Connection
Zeko SQL Builder provides a standard way to connect to your DB to execute the queries. 
Currently the DB connection pool is an abstraction on top of [HikariCP](https://github.com/brettwooldridge/HikariCP) and Vert.x JDBC client module.

## Creating DB Connection Pool and Session
First create a JasyncDBPool, HikariDBPool or VertxDBPool, you can refer to the [Vert.x JDBC client page](https://vertx.io/docs/vertx-jdbc-client/java/#_configuration) for the config. 
These classes are using the same configuration properties but not necessarily dependant on the Vert.x module. 

JasyncDBPool will automatically parse the jdbc url to populate the pool configuration if a JsonObject is passed with url field so you don't have to. 
You could still pass in a [ConnectionPoolConfigurationBuilder](https://github.com/jasync-sql/jasync-sql/wiki/Configuring-and-Managing-Connections) to its constructor for more control.

The pool object can be wrap into a class as a singleton. 
The connection pool and session are composed with suspend methods where you should be running them inside a coroutine.

```kotlin
import io.zeko.db.sql.connections.*
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

class DB {
    private var pool: HikariDBPool
    private var pool2: JasyncDBPool

    constructor() {
        val mysqlConfig = json {
            obj(
                "url" to "jdbc:mysql://localhost:3306/zeko_test?user=root&password=12345",
                "max_pool_size" to 30
            )
        }

        pool = HikariDBPool(config)
        // This is require if you want your Insert statement to return the newly inserted keys
        pool.setInsertStatementMode(Statement.RETURN_GENERATED_KEYS)
        pool2 = JasyncDBPool(config)
    }

    suspend fun session(): DBSession = HikariDBSession(pool, pool.createConnection())
    suspend fun session2(): DBSession = JasyncDBSession(pool2, pool2.createConnection())
}
```

In your model code, you can call the DB instance you just created and start execute queries
The once block will execute the query and then close the connection at the end of the block.
```kotlin
    suspend fun queryMe(statusFlag: Int) {
        val sql = Query().fields("*").from("user").where("status" eq statusFlag).toSql()

        db.session().once { conn ->
            val result = conn.queryPrepared(sql, listOf(statusFlag)) as java.sql.ResultSet
        }
    }
```
If you are using VertxDBPool you should cast the result to as io.vertx.ext.sql.ResultSet
If you are using JasyncDBPool you should cast the result to as [com.github.jasync.sql.db.QueryResult](https://github.com/jasync-sql/jasync-sql/wiki/Executing-Statements#query)

#### Retries, 
The DB session allows you to retry the same statements. 
Calling retry(2) instead of once will execute the code block additional 2 times if it failed
```kotlin
    suspend fun queryMe(statusFlag: Int) {
        val sql = Query().fields("*").from("user").where("status" eq statusFlag).toSql()

        db.session().retry(2) { conn ->
            val result = conn.queryPrepared(sql, listOf(statusFlag)) as java.sql.ResultSet
        }
    }
```

The retry can be delayed by passing a second parameter as miliseconds. This will delay 500ms before the retry will be executed
```kotlin
db.session().retry(2, 500) { conn ->
    val result = conn.queryPrepared(sql, listOf(statusFlag)) as java.sql.ResultSet
}
```

#### Transactions
Transactions has the same parameters as retry(numRetries, delay), the only difference is the queries will be executed as a transaction block. 
Any exceptions will result in a rollback automatically. Connection is closed as well at the end of the block.
If you do not want the connection to be close, call transactionOpen instead.
```kotlin
db.session().transaction { conn ->
    val result = conn.queryPrepared(sql, listOf(statusFlag)) as java.sql.ResultSet
    val sqlInsert = """INSERT INTO user (name, email) VALUES (?, ?)"""
    val ids = conn.insert(sqlInsert, arrayListOf(
                    "User " + System.currentTimeMillis(),
                    "abc@gmail.com"
            )) as List<Int>
}
```

The example insert returns a List of inserted IDs, this can only work if you have set beforehand:
```kotlin
pool.setInsertStatementMode(Statement.RETURN_GENERATED_KEYS)
```
Note: Not all database works with this, for instance Apache Ignite will throw exception since it does not support this SQL feature.
Jasync driver on the other hand [only works with MySQL](https://github.com/jasync-sql/jasync-sql/wiki/FAQ#q-i-inserted-a-row-how-do-i-get-an-auto-incremented-id) (this high performance non-blocking driver is for MySQL and PostgreSQL only)

#### Duplicate Entries
If you have inserted/updated a duplicated entry due to unique index or primary key, the DB client will throw DuplicateKeyException by default.
```kotlin
import io.zeko.db.sql.exceptions.DuplicateKeyException

try {
    conn.insert(sqlInsert, listOf("User Name", "abc@gmail.com"))
} catch (err: DuplicateKeyException) {
    if (err.equals("email")) {
        println("email column: Email address is already registered")
    }
}
```

#### More controls on connection
To execute the queries with more control you can get the underlying connection object by calling rawConnection:
```kotlin
    val sess = db.session()
    val conn = sess.rawConnection() as java.sql.Connection
    // For Vert.x jdbc client, it will be:
    // sess.rawConnection() as io.vertx.ext.sql.SQLConnection
    
    // Now you can do whatever you want, though the transaction{} block actually does this automatically
    conn.autoCommit = false
    lateinit var rows: List<User>

    try {
        val sql = "SELECT * FROM user WHERE status = ? ORDER BY id ASC"
        rows = sess.queryPrepared(sql, listOf(1), { User(it) }) as List<User>
        conn.commit()
    } catch (err: Exception) {
        conn.rollback()
    } finally {
        conn.close()
    }
```

## Result Set Mapping to POJO/Entity
The query method accepts a lambda where you can process the raw data map to the POJO/entity class you need.
In this case User class is just a class with map delegate to its properties
```kotlin
class User : Entity {
    constructor(map: Map<String, Any?>) : super(map)
    constructor(vararg props: Pair<String, Any?>) : super(*props)
    var id: Int?     by map
    var name: String? by map
}

suspend fun queryUsers(statusFlag: Int): List<User> {
    lateinit var rows: List<User>
    db.session().once { conn ->
        val sql = "SELECT * FROM user WHERE status = ? ORDER BY id ASC"
        rows = sess.queryPrepared(sql, listOf(1), { User(it) }) as List<User>
    }
}
return rows
```

## Entity Property Type Mapping
There are times when working with different databases or different DB drivers, the java type conversion might be different from what you need.
For instance, with a MySQL DATETIME column, Jasync returns Joda LocalDateTime, Hikari gives java.sql.Timestamp, Vert.x JDBC client return as String(auto converted to UTC timezone)
While with TINYINT, Hikari and Vert.x return Boolean, Jasync returns Byte.

In order to map the property to the type you need, override propTypeMapping with a map of property names to its relevant type.
```kotlin
import io.zeko.model.Entity
import io.zeko.model.Type

class User : Entity {
    constructor(map: Map<String, Any?>) : super(map)
    constructor(vararg props: Pair<String, Any?>) : super(*props)

    override fun propTypeMapping() = mapOf(
        "status" to Type.INT,
        "subscribed" to Type.BOOL,
        "lastAccessAt" to Type.ZONEDATETIME_UTC, //gives ZonedDateTime in UTC zone
        "createdAt" to Type.DATETIME,
        "dob" to Type.DATE  //gives LocalDate
    )

    var id: Int?     by map
    var firstName: String? by map
    var lastName: String? by map
    var subscribed: Boolean? by map
    var status: Int? by map
    var dob: LocalDate? by map
    var lastAccessAt: ZonedDateTime? by map
    var createdAt: LocalDateTime? by map
}
```

## Logging
You can use any logging framework of your preference, just create a log provider class by implementing the DBLogger Interface.
Example using Vert.x logger to log queries, retries blocks and sql errors.
```kotlin
import io.zeko.db.sql.connections.*
import io.vertx.core.*
import io.vertx.core.logging.LoggerFactory

class BootstrapVerticle : AbstractVerticle() {
    override fun start() {
        val logger = VertxDBLog(LoggerFactory.getLogger("app")).setParamsLogLevel(DBLogLevel.OFF)
        //...some db setup code
        val db = JasyncDBSession(pool, pool.createConnection()).setQueryLogger(logger)
    }
}

class VertxDBLog(val logger: Logger) : DBLogger {
    var paramsLevel: DBLogLevel = DBLogLevel.DEBUG
    var sqlLevel: DBLogLevel = DBLogLevel.DEBUG

    override fun logQuery(sql: String, params: List<Any?>?) {
        if (sqlLevel.level >= DBLogLevel.DEBUG.level) {
            logger.debug("[SQL] $sql $params")
        }
        if (paramsLevel.level >= DBLogLevel.DEBUG.level && params != null) {
            logger.debug("[SQL_PARAM] $params")
        }
    }
    override fun logRetry(numRetriesLeft: Int, err: Exception) { logger.warn("[SQL_RETRY:$numRetriesLeft] $err") }
    override fun logUnsupportedSql(err: Exception) { logger.warn("[SQL_UNSUPPORTED] $err") }
    override fun logError(err: Exception) { logger.error("[SQL_ERROR] $err") }
    override fun getParamsLogLevel(): DBLogLevel { return paramsLevel }
    override fun getSqlLogLevel(): DBLogLevel { return sqlLevel }
    override fun setParamsLogLevel(level: DBLogLevel): DBLogger { return this }
    override fun setSqlLogLevel(level: DBLogLevel): DBLogger { return this }
    override fun setLogLevels(sqlLevel: DBLogLevel, paramsLevel: DBLogLevel): DBLogger { return this }
}
```

## Data Mapper
If you are using [Zeko Data Mapper](https://github.com/darkredz/Zeko-Data-Mapper), you can write code as below to automatically map the objects to nested entities.

Example entities with User, Address and Role
```kotlin
import io.zeko.model.Entity

class User : Entity {
    constructor(map: Map<String, Any?>) : super(map)
    var id: Int?     by map
    var age: Int?     by map
    var name: String? by map
    var role_id: String? by map
    var role: List<Role>? by map
    var address: List<Address>? by map
}

class Address : Entity {
    constructor(map: MutableMap<String, Any?>) : super(map)
    var id: Int? by map
    var user_id: Int? by map
    var street1: String? by map
    var street2: String? by map
}

class Role : Entity {
    constructor(map: MutableMap<String, Any?>) : super(map)
    var id: Int?     by map
    var role_name: String? by map
    var user_id: Int? by map
}
```

Execute join queries where User has address and has many roles
```kotlin
    import io.zeko.db.sql.Query
    import io.zeko.db.sql.dsl.*
    import io.zeko.db.sql.aggregations.*
    import io.zeko.db.sql.operators.*
    import io.zeko.model.declarations.toMaps
    import io.zeko.model.DataMapper
    import io.zeko.model.TableInfo

    suspend fun mysqlQuery(): List<User> {
        val query = Query()
                .table("user").fields("id", "name")
                .table("role").fields("id", "role_name", "user.id = user_id")
                .table("address").fields("id", "street1", "street2", "user.id = user_id")
                .from("user")
                .leftJoin("address").on("user_id = user.id")
                .leftJoin("user_has_role").on("user_id = user.id")
                .leftJoin("role").on("id = user_has_role.role_id")
                .where(
                    between("user.id", 0, 1000)
                )
                .orderAsc("user.name")

        val (sql, columns) = query.compile(true)

        lateinit var rows: List<User>
        db.session().once {
            val result = it.query(sql, columns)
            rows = DataMapper().mapStruct(structUserProfile(), result) as List<User>
        }
        return rows
    }
```

```kotlin
    fun structUserProfile(): LinkedHashMap<String, TableInfo> {
        val tables = linkedMapOf<String, TableInfo>()
        tables["user"] = TableInfo(key = "id", dataClassHandler = { User(it) })
        tables["role"] = TableInfo(key = "id", dataClassHandler = { Role(it) }, move_under = "user", foreign_key = "user_id", many_to_many = true, remove = listOf("user_id"))
        tables["address"] = TableInfo(key = "id", dataClassHandler = { Address(it) }, move_under = "user", foreign_key = "user_id", many_to_one = true, remove = listOf("user_id"))
        return tables
    }
```
Example output json encode
```json
[{
        "id": 1,
        "name": "Bat Man",
        "role": [
            {
                "role_id": 2,
                "type": "super admin"
            }
        ],
        "address": [
            {
                "id": 1,
                "street1": "Jalan SS16/1",
                "street2": "Taman Tun"
            }
        ]
}]
```

## Performance
This is a simple benchmark with a rate of 2500 QPS on the query builder with result mapped across all three DB drivers(30 max connections pool)
<p align="center">
    <img src="./zeko-sql-builder-benchmark.jpeg" alt="Zeko Query Builder Benchmark" />
</p>
Hardware: MacBook Pro (13-inch, 2018, Four Thunderbolt 3 Ports) 2.3 GHz Intel Core i5 8 GB 2133 MHz LPDDR3


## Download
Add this to your maven pom.xml

    <dependency>
      <groupId>io.zeko</groupId>
      <artifactId>zeko-sql-builder</artifactId>
      <version>1.2.7</version>
    </dependency>
    
    <!-- Jasync Mysql driver if needed -->
    <dependency>
       <groupId>com.github.jasync-sql</groupId>
       <artifactId>jasync-mysql</artifactId>
       <version>1.1.5</version>
    </dependency>
    <!-- Hikari Mysql connection pool if needed -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>3.4.5</version>
    </dependency>
    <!-- Vertx jdbc client if needed -->
    <dependency>
        <groupId>io.vertx</groupId>
        <artifactId>vertx-jdbc-client</artifactId>
        <version>${vertx.version}</version>
    </dependency>
    <dependency>
        <groupId>org.jetbrains.kotlinx</groupId>
        <artifactId>kotlinx-coroutines-core</artifactId>
        <version>1.3.3</version>
    </dependency>
