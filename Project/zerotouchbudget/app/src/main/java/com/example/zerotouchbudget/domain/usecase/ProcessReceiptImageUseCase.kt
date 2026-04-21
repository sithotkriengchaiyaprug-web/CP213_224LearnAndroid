package com.example.zerotouchbudget.domain.usecase

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
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
    suspend operator fun invoke(bitmap: Bitmap?): Result<Transaction> = runCatching {
        if (bitmap == null) throw Exception("Bitmap is null")

        // 1. Resize Image (Best Practice)
        val resizedBitmap = resizeBitmap(bitmap, 1024)
        
        // 2. Explicit Prompt
        val prompt = """
            Analyze the attached image of a receipt or bank slip.
            
            Extract the following info:
            - total amount (as a number)
            - merchant/store name (as brand)

            Rules:
            - Return ONLY valid JSON.
            - No markdown (no ```json).
            - No explanations.

            Format: {"amount": 0.0, "brand": "Name"}
        """.trimIndent()

        // 3. Multimodal Input
        val response = generativeModel.generateContent(content {
            image(resizedBitmap)
            text(prompt)
        })

        val rawText = response.text ?: throw Exception("Empty response from Gemini")
        Log.d("GeminiScan", "Raw Response: ${rawText}")

        // 4. Robust JSON Parsing
        val cleanText = rawText
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val jsonMatch = Regex("\\{.*\\}").find(cleanText.replace("\n", ""))
            ?: throw Exception("Invalid JSON format from AI")

        val json = JSONObject(jsonMatch.value)
        val amount = json.optDouble("amount", 0.0)
        val brand = json.optString("brand", "Unknown").trim()

        // 5. Validation
        if (amount <= 0.0) throw Exception("Could not extract valid amount")

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            brand = brand,
            category = mapCategory(brand),
            timestamp = System.currentTimeMillis(),
            source = TransactionSource.OCR,
            note = "Scanned via Gemini AI"
        )

        repository.insertTransaction(transaction)
        transaction
    }

    private fun resizeBitmap(source: Bitmap, maxWidth: Int): Bitmap {
        if (source.width <= maxWidth) return source
        val aspectRatio = source.height.toDouble() / source.width.toDouble()
        val targetHeight = (maxWidth * aspectRatio).toInt()
        return Bitmap.createScaledBitmap(source, maxWidth, targetHeight, true)
    }

    private fun mapCategory(brand: String): String {
        val b = brand.lowercase()
        return when {
            listOf("starbucks", "cafe", "amazon").any { b.contains(it) } -> "Drinks"
            listOf("7-11", "seven", "lotus", "tops").any { b.contains(it) } -> "Groceries"
            listOf("grab", "lineman", "foodpanda").any { b.contains(it) } -> "Food Delivery"
            listOf("shell", "ptt", "bts", "mrt").any { b.contains(it) } -> "Transport"
            else -> "General"
        }
    }
}
