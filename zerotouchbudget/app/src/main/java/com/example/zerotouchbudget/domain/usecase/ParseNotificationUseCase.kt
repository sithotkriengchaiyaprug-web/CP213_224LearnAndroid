package com.example.zerotouchbudget.domain.usecase

import com.example.zerotouchbudget.domain.model.Transaction
import com.example.zerotouchbudget.domain.model.TransactionSource
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject

class ParseNotificationUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(title: String, text: String, packageName: String) {
        val amount = extractAmount(text) ?: return
        val brand = extractBrand(text, title)
        val category = mapCategory(brand, text)

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            brand = brand,
            category = category,
            timestamp = System.currentTimeMillis(),
            source = TransactionSource.NOTIFICATION,
            note = "Auto-detected from $packageName"
        )

        repository.insertTransaction(transaction)
    }

    private fun extractAmount(text: String): Double? {
        val candidates = mutableListOf<Double>()
        val patterns = listOf(
            Regex("(?i)(?:amount|paid|transfer|ยอดเงิน|จำนวนเงิน|ชำระเงิน)\\s*:?\\s*([0-9,]+\\.?[0-9]*)"),
            Regex("(?i)THB\\s*([0-9,]+\\.?[0-9]*)"),
            Regex("([0-9,]+\\.?[0-9]*)\\s*(?i)(?:บาท|baht|THB)")
        )

        for (regex in patterns) {
            regex.findAll(text).forEach { match ->
                val value = match.groupValues[1]
                    .replace(",", "")
                    .toDoubleOrNull()

                if (value != null && value in 1.0..100000.0) {
                    candidates.add(value)
                }
            }
        }
        return candidates.maxOrNull()
    }

    private fun extractBrand(text: String, title: String): String {
        val patterns = listOf(
            Regex("(?i)(?:at|@|to|ที่ร้าน|โอนไปยัง)\\s+([^\\d\\n]+)"),
            Regex("(?i)(?:ร้าน|merchant)\\s*:?\\s*([^\\d\\n]+)")
        )

        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                return cleanBrand(match.groupValues[1])
            }
        }
        return cleanBrand(title)
    }

    private fun cleanBrand(raw: String): String {
        return raw
            .replace(Regex("[^\\u0E00-\\u0E7Fa-zA-Z0-9 ]"), "")
            .replace(Regex("(?i)(k-plus|scb|krungthai|bbl|kma)"), "")
            .trim()
            .ifEmpty { "Unknown" }
    }

    private fun mapCategory(brand: String, text: String): String {
        val b = brand.lowercase()
        val t = text.lowercase()

        return when {
            listOf("starbucks", "cafe", "amazon").any { b.contains(it) } -> "Drinks"
            listOf("7-11", "seven", "lotus", "tops").any { b.contains(it) } -> "Groceries"
            listOf("grab", "lineman", "foodpanda").any { b.contains(it) } -> "Food Delivery"
            listOf("shell", "ptt", "bts", "mrt").any { b.contains(it) } -> "Transport"
            listOf("electric", "water", "internet").any { t.contains(it) } -> "Bills"
            else -> "General"
        }
    }
}
