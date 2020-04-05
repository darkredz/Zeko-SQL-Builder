package io.zeko.db.sql

abstract class Sort {
    var fieldName: String = ""

    constructor(fieldName: String) {
        this.fieldName = fieldName
    }
    open fun getDirection(): String = "ASC"
}
