package io.zeko.db.sql

class And : Condition {
    constructor(logic: String) : super(logic)
    override fun getOperator(): String = "AND"
}
