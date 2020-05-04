package io.zeko.db.sql.exceptions

import java.lang.Exception

fun throwDuplicate(err: Exception) {
    if (err.message!!.contains("duplicate", true)) {
        var column: String? = null
        var entry: String? = null

        //MySQL
        if (err.message!!.contains("Duplicate entry '")) {
            val rgxFindField = "\\'([^\\']+)\\'".toPattern()
            val matcher = rgxFindField.matcher(err.message)

            while (matcher.find()) {
                if (entry == null) {
                    entry = matcher.group(1)
                } else {
                    column = matcher.group(1)
                }
            }
        }
        //Apache Ignite/Ansi
        else if (err.message!!.startsWith("Duplicate key during")) {
            val rgxFindField = "\\[([^\\[\\]]+)\\]".toPattern()
            val matcher = rgxFindField.matcher(err.message)
            var str = ""
            while (matcher.find()) {
                str = matcher.group(1)
                break
            }
            val parts = str.removePrefix("[").removeSuffix("]").split("\\=".toRegex(), 2)
            column = if (parts[0] == "key") "PRIMARY" else parts[0]
            entry = parts[1]
        }

        throw DuplicateKeyException(column + "", entry + "", err.message)
    }
}
