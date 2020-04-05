package io.zeko.db.sql

abstract class Condition {
    private var logic: String = ""

    constructor(logic: String) {
        this.logic = logic
    }

    open fun getStatement(): String = logic
    open fun getOperator(): String = ""
}
