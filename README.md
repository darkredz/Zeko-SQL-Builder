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

```kotlin
import io.zeko.db.sql.Query

Query().fields("*").from("user").toSql()
Query().fields("id", "name", "age").from("user").toSql()

```

Outputs:


##Download

    <dependency>
      <groupId>io.zeko</groupId>
      <artifactId>zeko-data-mapper</artifactId>
      <version>1.5.10</version>
    </dependency>
    
