package com.example.zerotouchbudget.domain.usecase

import android.graphics.Bitmap
import android.util.Log
import com.example.zerotouchbudget.domain.model.Transaction
import com.example.zerotouchbudget.domain.model.TransactionSource
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

class ProcessReceiptImageUseCase @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val repository: TransactionRepository
) {

    suspend operator fun invoke(bitmap: Bitmap?): Result<Transaction> = runCatching {
        requireNotNull(bitmap) { "Bitmap is null" }

        withContext(Dispatchers.IO) {
            Log.d(TAG, "Starting receipt processing")
            Log.d(TAG, "Input bitmap: ${bitmap.width}x${bitmap.height}, bytes=${bitmap.byteCount}")

            val resizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_WIDTH)
            Log.d(TAG, "Bitmap ready for Gemini: ${resizedBitmap.width}x${resizedBitmap.height}")

            val response = callGeminiWithTimeout(resizedBitmap, buildOptimizedPrompt())
            val rawText = response.text?.trim().orEmpty()

            Log.d(TAG, "AI raw response: $rawText")
            if (rawText.isBlank()) {
                throw IllegalStateException("Gemini returned an empty response")
            }

            val extracted = parseReceiptData(rawText)
            Log.d(
                TAG,
                "Parsed receipt data: amount=${extracted.amount}, brand=${extracted.brand}, " +
                    "confidence=${extracted.confidence}, parser=${extracted.parserMode}"
            )

            if (extracted.amount <= 0.0) {
                throw IllegalStateException(
                    "Invalid amount extracted from AI response. " +
                        "Fallback parsing also failed. rawResponseLength=${rawText.length}"
                )
            }

            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                amount = extracted.amount,
                brand = extracted.brand,
                category = mapCategory(extracted.brand),
                timestamp = System.currentTimeMillis(),
                source = TransactionSource.OCR,
                note = "Scanned via Gemini AI (confidence=${extracted.confidence}, parser=${extracted.parserMode})"
            )

            Log.d(
                TAG,
                "Transaction created: id=${transaction.id}, amount=${transaction.amount}, " +
                    "brand=${transaction.brand}, category=${transaction.category}"
            )

            Log.d(TAG, "Inserting transaction into database...")
            repository.insertTransaction(transaction)
            Log.d(TAG, "Database insert complete: id=${transaction.id}")

            transaction
        }
    }

    private suspend fun callGeminiWithTimeout(
        bitmap: Bitmap,
        prompt: String
    ) = withTimeoutOrNull(TIMEOUT_MS) {
        Log.d(TAG, "Calling Gemini API...")
        generativeModel.generateContent(
            content {
                image(bitmap)
                text(prompt)
            }
        )
    } ?: throw IllegalStateException("Gemini API timeout after ${TIMEOUT_MS / 1000} seconds")

    private fun buildOptimizedPrompt(): String = """
        You are a receipt OCR extraction system.
        Return ONLY valid JSON with this exact schema:
        {"amount": number, "brand": "string", "confidence": "high|medium|low"}

        Rules:
        - amount must be the final payable total
        - brand should be the merchant/store/bank name
        - if amount is unclear, return 0.0
        - if brand is unclear, return "Unknown"
        - no markdown, no code fences, no extra text
    """.trimIndent()

    private fun parseReceiptData(rawText: String): ReceiptExtractionData {
        val cleaned = stripCodeFences(rawText)
        Log.d(TAG, "Cleaned AI text: $cleaned")

        val jsonObject = parseJsonObject(cleaned)
        if (jsonObject != null) {
            val extractedFromJson = extractFromJson(jsonObject, parserMode = "json")
            if (extractedFromJson.amount > 0.0) {
                return extractedFromJson
            }

            Log.w(TAG, "JSON parse succeeded but amount is invalid. Trying text fallback.")
        } else {
            Log.w(TAG, "JSON parse failed. Trying text fallback.")
        }

        val fallback = extractFromText(cleaned)
        if (fallback.amount > 0.0) {
            Log.w(TAG, "Fallback text parser succeeded.")
        }
        return fallback
    }

    private fun parseJsonObject(text: String): JSONObject? {
        val directParse = runCatching { JSONObject(text) }.onFailure {
            Log.d(TAG, "Direct JSON parse failed: ${it.message}")
        }.getOrNull()
        if (directParse != null) return directParse

        val substring = extractJsonSubstring(text)
        if (substring.isBlank()) {
            return null
        }

        return runCatching { JSONObject(substring) }.onFailure {
            Log.d(TAG, "Substring JSON parse failed: ${it.message}")
        }.getOrNull()
    }

    private fun stripCodeFences(text: String): String {
        return text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun extractJsonSubstring(text: String): String {
        val startIndex = text.indexOf('{')
        if (startIndex == -1) {
            return ""
        }

        var braceCount = 0
        var inString = false
        var escaped = false

        for (index in startIndex until text.length) {
            val char = text[index]

            if (escaped) {
                escaped = false
                continue
            }

            if (char == '\\') {
                escaped = true
                continue
            }

            if (char == '"' ) {
                inString = !inString
                continue
            }

            if (!inString) {
                when (char) {
                    '{' -> braceCount++
                    '}' -> {
                        braceCount--
                        if (braceCount == 0) {
                            return text.substring(startIndex, index + 1)
                        }
                    }
                }
            }
        }

        return text.substring(startIndex)
    }

    private fun extractFromJson(json: JSONObject, parserMode: String): ReceiptExtractionData {
        val amount = readAmount(
            json.opt("amount"),
            json.opt("total"),
            json.opt("grand_total"),
            json.opt("value"),
            json.opt("transactionAmount"),
            json.opt("finalAmount")
        )
        val brand = readString(
            json.opt("brand"),
            json.opt("merchant"),
            json.opt("store"),
            json.opt("merchant_name"),
            json.opt("name"),
            json.opt("vendor")
        )
        val confidence = readString(
            json.opt("confidence"),
            json.opt("score")
        ).let(::normalizeConfidence)

        return ReceiptExtractionData(
            amount = amount,
            brand = normalizeBrand(brand),
            confidence = confidence,
            parserMode = parserMode
        )
    }

    private fun extractFromText(rawText: String): ReceiptExtractionData {
        val amount = extractAmountFromText(rawText)
        val brand = extractBrandFromText(rawText)
        return ReceiptExtractionData(
            amount = amount,
            brand = normalizeBrand(brand),
            confidence = if (amount > 0.0) "low" else "low",
            parserMode = "fallback-text"
        )
    }

    private fun extractAmountFromText(text: String): Double {
        val keywordPatterns = listOf(
            Regex("(?i)(?:total|amount due|grand total|net total|final total|payable)[^0-9]{0,24}([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"),
            Regex("(?i)(?:฿|thb|บาท)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)")
        )

        for (pattern in keywordPatterns) {
            val match = pattern.find(text) ?: continue
            parseAmount(match.groupValues[1])?.let { amount ->
                if (amount > 0.0) {
                    return amount
                }
            }
        }

        val numericMatches = Regex("""\b\d[\d,]*(?:\.\d{1,2})?\b""")
            .findAll(text)
            .mapNotNull { parseAmount(it.value) }
            .filter { it > 0.0 }
            .toList()

        return numericMatches.maxOrNull() ?: 0.0
    }

    private fun extractBrandFromText(text: String): String {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val candidate = lines.take(6).firstOrNull { line ->
            line.any(Char::isLetter) && !containsAmountKeywords(line)
        } ?: lines.firstOrNull { it.any(Char::isLetter) }

        return candidate?.replace(Regex("\\s+"), " ")?.take(80) ?: "Unknown"
    }

    private fun containsAmountKeywords(line: String): Boolean {
        val lower = line.lowercase()
        return listOf("total", "amount", "paid", "change", "balance", "subtotal", "vat")
            .any { lower.contains(it) }
    }

    private fun readAmount(vararg candidates: Any?): Double {
        for (candidate in candidates) {
            when (candidate) {
                is Number -> {
                    val amount = candidate.toDouble()
                    if (amount > 0.0) return amount
                }
                is String -> {
                    parseAmount(candidate)?.let { amount ->
                        if (amount > 0.0) return amount
                    }
                }
            }
        }
        return 0.0
    }

    private fun readString(vararg candidates: Any?): String {
        for (candidate in candidates) {
            val value = when (candidate) {
                is String -> candidate.trim()
                else -> candidate?.toString()?.trim().orEmpty()
            }
            if (value.isNotBlank() && value != "null") {
                return value
            }
        }
        return "Unknown"
    }

    private fun parseAmount(raw: String): Double? {
        val cleaned = raw
            .replace(",", "")
            .replace("฿", "")
            .replace("บาท", "")
            .replace("THB", "", ignoreCase = true)
            .trim()

        return cleaned.toDoubleOrNull()
    }

    private fun normalizeBrand(value: String): String {
        return value.trim().ifEmpty { "Unknown" }
    }

    private fun normalizeConfidence(value: String): String {
        return when (value.trim().lowercase()) {
            "high", "medium", "low" -> value.trim().lowercase()
            else -> "low"
        }
    }

    private fun resizeBitmap(source: Bitmap, maxWidth: Int = MAX_IMAGE_WIDTH): Bitmap {
        if (source.width <= maxWidth) return source

        val aspectRatio = source.height.toDouble() / source.width.toDouble()
        val targetHeight = (maxWidth * aspectRatio).toInt()

        Log.d(TAG, "Resizing bitmap: ${source.width}x${source.height} -> ${maxWidth}x$targetHeight")
        return Bitmap.createScaledBitmap(source, maxWidth, targetHeight, true)
    }

    private fun mapCategory(brand: String): String {
        val normalized = brand.lowercase()
        return when {
            listOf("starbucks", "coffee", "cafe", "drink").any { normalized.contains(it) } -> "Drinks"
            listOf("amazon", "mall", "shopping", "shopee").any { normalized.contains(it) } -> "Shopping"
            listOf("grab", "lineman", "foodpanda", "delivery").any { normalized.contains(it) } -> "Food Delivery"
            listOf("shell", "bts", "mrt", "taxi").any { normalized.contains(it) } -> "Transport"
            listOf("bank", "wallet", "money", "promptpay", "true").any { normalized.contains(it) } -> "Finance"
            listOf("electric", "water", "gas", "wifi", "internet").any { normalized.contains(it) } -> "Utilities"
            else -> "General"
        }
    }

    private data class ReceiptExtractionData(
        val amount: Double,
        val brand: String,
        val confidence: String,
        val parserMode: String
    )

    companion object {
        private const val TAG = "ProcessReceiptOCR"
        private const val MAX_IMAGE_WIDTH = 1536
        private const val TIMEOUT_MS = 30_000L
    }
}
