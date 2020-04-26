package io.zeko.db.sql.exceptions

import java.lang.Exception

fun throwDuplicate(err: Exception) {
    if (err.message!!.contains("Duplicate")) {
        val rgxFindField = "\\'([^\\']+)\\'".toPattern()
        val matcher = rgxFindField.matcher(err.message)
        var column: String? = null
        var entry: String? = null

        while (matcher.find()) {
            if (entry == null) {
                entry = matcher.group(1)
            } else {
                column = matcher.group(1)
            }
        }
        throw DuplicateKeyException(column + "", entry+ "", err.message)
    }
}
