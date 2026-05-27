package com.example.data

object CustomJsonHelper {
    fun toJson(items: List<VoucherItem>): String {
        return "[" + items.joinToString(",") { """{"num":"${it.num}","amt":${it.amt}}""" } + "]"
    }

    fun fromJson(json: String): List<VoucherItem> {
        val items = mutableListOf<VoucherItem>()
        // Pattern matches both compact {"num":"12","amt":100} and formatted json
        val regex = Regex("""\{\s*"num"\s*:\s*"([^"]+)"\s*,\s*"amt"\s*:\s*(\d+)\s*\}""")
        val matches = regex.findAll(json)
        for (match in matches) {
            val num = match.groupValues[1]
            val amt = match.groupValues[2].toIntOrNull() ?: 0
            items.add(VoucherItem(num, amt))
        }
        return items
    }
}
