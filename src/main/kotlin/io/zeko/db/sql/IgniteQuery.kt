package io.zeko.db.sql

class IgniteQuery : Query {
    constructor(
            espChar: String =  "\"",
            asChar: String = "=",
            espTableName: Boolean = true
    ) : super(espChar, asChar, espTableName)
}
