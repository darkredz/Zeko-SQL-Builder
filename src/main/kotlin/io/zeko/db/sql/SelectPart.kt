package io.zeko.db.sql

data class SelectPart (
    val columns: List<String>,
    val sqlFields: String
)
