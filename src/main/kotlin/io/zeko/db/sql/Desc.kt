package io.zeko.db.sql

class Desc : Sort {
    constructor(fieldName: String) : super(fieldName)

    override fun getDirection(): String {
        return "DESC"
    }
}
