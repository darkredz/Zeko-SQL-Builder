package io.zeko.db.sql

class Or : Condition {
    constructor(logic: String) : super(logic)
    override fun getOperator(): String = "OR"
}
