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
        val amount = extractAmount(text)
        if (amount == null) return
        val brand = extractBrand(text, title)

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            brand = brand,
            category = "Uncategorized",
            timestamp = System.currentTimeMillis(),
            source = TransactionSource.NOTIFICATION,
            note = "Auto-detected from " + packageName
        )

        repository.insertTransaction(transaction)
    }

    private fun extractAmount(text: String): Double? {
        val regex1 = Regex("(?i)(?:amount|paid|transfer)\\s*:?\\s*([0-9,]+\\.?[0-9]*)")
        val regex2 = Regex("(?i)THB\\s*([0-9,]+\\.?[0-9]*)")
        val regex3 = Regex("([0-9,]+\\.?[0-9]*)\\s*(?i)(?:THB|baht)")

        var match = regex1.find(text)
        if (match == null) match = regex2.find(text)
        if (match == null) match = regex3.find(text)
        if (match == null) return null

        val rawValue: String = match.groupValues.get(1)
        val sb = StringBuilder()
        for (i in 0 until rawValue.length) {
            val c = rawValue[i]
            if (c != ',') {
                sb.append(c)
            }
        }
        return sb.toString().toDoubleOrNull()
    }

    private fun extractBrand(text: String, title: String): String {
        val regex = Regex("(?i)(?:at|@)\\s+(.+?)(?:\\s|$)")
        val match = regex.find(text)
        if (match != null) {
            val brand: String = match.groupValues.get(1)
            return brand.trim()
        }
        return title.take(30)
    }
}