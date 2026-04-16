package com.example.zerotouchbudget.domain.usecase

import android.graphics.Bitmap
import com.example.zerotouchbudget.domain.model.Transaction
import com.example.zerotouchbudget.domain.model.TransactionSource
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

class ProcessReceiptImageUseCase @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): Result<Transaction> = runCatching {
        val prompt = """
            Analyze this receipt. Extract the total amount and the store/brand name.
            Return ONLY a valid JSON object in this exact format:
            {"amount": 150.50, "brand": "Starbucks"}
            Do not include markdown formatting, backticks, or any other text.
        """.trimIndent()

        val response = generativeModel.generateContent(content {
            image(bitmap)
            text(prompt)
        })

        val jsonText = response.text ?: throw Exception("Empty response from Gemini")
        val jsonObject = JSONObject(jsonText)

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = jsonObject.getDouble("amount"),
            brand = jsonObject.getString("brand"),
            category = "Uncategorized",
            timestamp = System.currentTimeMillis(),
            source = TransactionSource.OCR,
            note = "Scanned from receipt"
        )

        repository.insertTransaction(transaction)
        transaction
    }
}