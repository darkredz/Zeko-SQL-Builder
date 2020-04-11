package io.zeko.db.sql.utilities


fun String.toSnakeCase(): String {
    var text: String = ""
    this.forEachIndexed { index, c ->
        if (c.isUpperCase()) {
            if (index > 0) text += "_"
            text += c.toLowerCase()
        } else {
            text += c
        }
    }
    return text
}
