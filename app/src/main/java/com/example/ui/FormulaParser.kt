package com.example.ui

import com.example.data.VoucherItem

class FormulaParser {
    
    fun isValidInput(line: String): Boolean {
        if (!line.contains('=')) return false
        val parts = line.split('=')
        if (parts.size != 2) return false
        val left = parts[0].trim()
        val right = parts[1].trim()
        if (left.isEmpty() || right.isEmpty()) return false
        
        val cleanVal = right.replace("r", "", ignoreCase = true).trim()
        if (cleanVal.isEmpty()) return false
        
        // Handle r-splitting, e.g. 100r50 (not starting with r)
        if (right.contains('r', ignoreCase = true) && !right.startsWith("r", ignoreCase = true)) {
            val rParts = right.lowercase().split('r')
            if (rParts.size == 2) {
                return rParts[0].trim().toIntOrNull() != null && rParts[1].trim().toIntOrNull() != null
            }
            return false
        }
        
        return cleanVal.toIntOrNull() != null
    }

    fun parseLine(line: String, use19: Boolean): List<VoucherItem> {
        if (!isValidInput(line)) return emptyList()
        
        val voucherItems = mutableListOf<VoucherItem>()
        val parts = line.split('=')
        val left = parts[0].trim()
        val right = parts[1].trim()

        fun safeInt(v: String): Int {
            val clean = v.replace(Regex("[^0-9]"), "")
            return clean.toIntOrNull() ?: 0
        }

        val rightLower = right.lowercase()
        
        if (rightLower.contains('r') && !rightLower.startsWith('r')) {
            val rParts = rightLower.split('r')
            val amt1 = safeInt(rParts[0])
            val amt2 = safeInt(rParts[1])
            parseNumbers(left).forEach { num ->
                voucherItems.add(VoucherItem(num, amt1))
                voucherItems.add(VoucherItem(reverseNumber(num), amt2))
            }
        } else if (rightLower.startsWith('r')) {
            val amt = safeInt(rightLower.replace("r", ""))
            parseNumbers(left).forEach { num ->
                voucherItems.add(VoucherItem(num, amt))
                voucherItems.add(VoucherItem(reverseNumber(num), amt))
            }
        } else if (left.contains("ပတ်")) {
            val amt = safeInt(right)
            val digits = left.replace("ပတ်", "").replace(".", "").replace(" ", "").trim()
            val list = mutableListOf<String>()
            digits.forEach { char ->
                val digit = char.toString()
                for (j in 0..9) {
                    list.add(digit + j)
                    list.add(j.toString() + digit)
                }
            }
            val finalNums = if (use19) list.distinct() else list
            finalNums.forEach { num ->
                voucherItems.add(VoucherItem(num, amt))
            }
        } else if (left.contains("ထိပ်") || left.contains("နောက်")) {
            val amt = safeInt(right)
            val isThip = left.contains("ထိပ်")
            val typeStr = if (isThip) "ထိပ်" else "နောက်"
            val digits = left.replace(typeStr, "").replace(".", "").replace(" ", "").trim()
            digits.forEach { char ->
                val digit = char.toString()
                for (j in 0..9) {
                    val num = if (isThip) (digit + j) else (j.toString() + digit)
                    voucherItems.add(VoucherItem(num, amt))
                }
            }
        } else if (left.contains("ခွေ")) {
            val amt = safeInt(right)
            val digits = left.replace("ခွေ", "").replace(".", "").replace(" ", "").trim()
            for (x in 0 until digits.length) {
                for (y in (x + 1) until digits.length) {
                    val d1 = digits[x].toString()
                    val d2 = digits[y].toString()
                    voucherItems.add(VoucherItem(d1 + d2, amt))
                    voucherItems.add(VoucherItem(d2 + d1, amt))
                }
            }
        } else if (left.contains("အပူး") || left.contains("ညီကို") || left.contains("ပါဝါ") || left.contains("နက္ခတ်")) {
            val amt = safeInt(right)
            getQuickNums(left).forEach { num ->
                voucherItems.add(VoucherItem(num, amt))
            }
        } else {
            val amt = safeInt(right)
            left.split('.').forEach { piece ->
                val num = piece.trim()
                if (num.length == 2 && num.all { it.isDigit() }) {
                    voucherItems.add(VoucherItem(num, amt))
                }
            }
        }
        return voucherItems
    }

    private fun parseNumbers(left: String): List<String> {
        val list = mutableListOf<String>()
        left.split('.').forEach { piece ->
            val num = piece.trim()
            if (num.length == 2 && num.all { it.isDigit() }) {
                list.add(num)
            }
        }
        return list
    }

    private fun reverseNumber(num: String): String {
        return if (num.length == 2) num.reversed() else num
    }

    private fun getQuickNums(key: String): List<String> {
        return when {
            key.contains("အပူး") -> listOf("00","11","22","33","44","55","66","77","88","99")
            key.contains("ညီကို") -> listOf("01","12","23","34","45","56","67","78","89","90","10","21","32","43","54","65","76","87","98","09")
            key.contains("ပါဝါ") -> listOf("05","50","16","61","27","72","38","83","49","94")
            key.contains("နက္ခတ်") -> listOf("07","70","18","81","24","42","35","53","69","96")
            else -> emptyList()
        }
    }
}
