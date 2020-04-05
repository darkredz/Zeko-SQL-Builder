package io.zeko.db.sql

class Asc : Sort {
    constructor(fieldName: String) : super(fieldName)

    override fun getDirection(): String {
        return "ASC"
    }
}
