package io.zeko.db.sql

class ANSIQuery : Query {
    constructor(
            espChar: String =  "\"",
            asChar: String = "=",
            espTableName: Boolean = true
    ) : super(espChar, asChar, espTableName)
}
