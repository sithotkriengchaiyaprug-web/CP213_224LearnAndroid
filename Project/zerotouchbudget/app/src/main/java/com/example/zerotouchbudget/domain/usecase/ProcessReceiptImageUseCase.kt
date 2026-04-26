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

/** Thrown when the image is not a receipt (AI returned amount=0 or invalid) */
class NotAReceiptException(reason: String) : Exception("Not a receipt: $reason")

/** Thrown when Gemini API quota/rate limit is exceeded — stop entire batch */
class RateLimitException(message: String) : Exception(message)

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

            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Bangkok")
            }
            val currentDate = format.format(java.util.Date())

            val response = callGeminiWithTimeout(resizedBitmap, buildOptimizedPrompt(currentDate))
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

            val safeBrand = extracted.brand.ifBlank { "Unknown" }

            if (extracted.amount <= 0.0 || extracted.amount > 1_000_000.0) {
                // ไม่ใช่สลิป หรือ AI อ่านยอดไม่ได้ → silent failure
                throw NotAReceiptException("amount=${extracted.amount}")
            }

            val baseNote = "Scanned via Gemini AI (confidence=${extracted.confidence}, parser=${extracted.parserMode})"
            var finalNote = if (extracted.needsReview) "[รอ user ยืนยัน] $baseNote" else baseNote
            if (extracted.isTimeMissing) {
                finalNote += " (เวลาบนสลิปไม่ชัดเจน)"
            }
            if (extracted.isDateMissing) {
                finalNote += " (วันที่และเวลาไม่ชัดเจน ใช้เวลาสแกนแทน)"
            }

            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                amount = extracted.amount,
                brand = safeBrand,
                category = mapCategory(safeBrand),
                timestamp = extracted.timestamp ?: com.example.zerotouchbudget.domain.util.DateUtils.nowUtcMillis(),
                source = TransactionSource.OCR,
                note = finalNote
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

    private fun buildOptimizedPrompt(currentDate: String): String = """
        You are a Thai bank transfer slip reader. 
        Extract structured data from this slip image and return ONLY valid JSON. 
        No explanation. No markdown. No extra text.
        
        Rules:
        - Today's date is $currentDate (use this to resolve ambiguous years)
        - Always return date in format: "YYYY-MM-DD" and time in "HH:mm" (24-hour, Asia/Bangkok timezone)
        - Convert Buddhist Era (พ.ศ.) to Christian Era (ค.ศ.) by subtracting 543
          Example: "25 เม.ย. 69" → date: "2026-04-25"
        - Map Thai month abbreviations:
          ม.ค.=01, ก.พ.=02, มี.ค.=03, เม.ย.=04, พ.ค.=05, มิ.ย.=06,
          ก.ค.=07, ส.ค.=08, ก.ย.=09, ต.ค.=10, พ.ย.=11, ธ.ค.=12
        - amount and fee must be numbers (not strings)
        - If date or time is unreadable, return null (do NOT guess)
        - If a field is unreadable or missing, use null
        - Do NOT guess or fill in missing data
        - status must be one of: "success" | "pending" | "failed" | "unknown"
        - bank must be one of: 
          "KBANK" | "SCB" | "KTB" | "BBL" | "BAY" | "TTB" | "GSB" | 
          "BAAC" | "CIMB" | "UOB" | "LH" | "TISCO" | "unknown"
        - channel must be one of: 
          "PromptPay" | "account_number" | "mobile" | "unknown"
        - transaction_type must be one of: "merchant" | "p2p" | "bill" | "unknown"
        
        Return this exact JSON structure:
        {
          "is_slip": true,
          "confidence": 0.97,
          "status": "success",
          "transaction_type": "merchant",
          "brand_name": "ร้านกาแฟนาย ก",
          "bank": "KBANK",
          "date": "2026-04-25",
          "time": "16:15",
          "sender_name": "นาย ก. ข.",
          "sender_account": "xxx-x-xxxx-x",
          "receiver_name": "ก. ข. ค.",
          "receiver_channel": "PromptPay",
          "receiver_account": "xxx-xxxxxxxx-xxxx",
          "ref_number": "XXXXXXXXXXXXXXXXXX",
          "amount": 50.00,
          "fee": 0.00,
          "currency": "THB",
          "note": null
        }
        
        If this image is NOT a bank slip at all, return:
        {
          "is_slip": false,
          "confidence": 0.95
        }
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
        if (json.has("is_slip") && !json.optBoolean("is_slip", true)) {
            Log.w(TAG, "Gemini determined this is NOT a bank slip.")
            throw IllegalStateException("Image is not a bank slip (is_slip: false)")
        }

        val amount = readAmount(
            json.opt("amount"),
            json.opt("total")
        )
        
        val transactionType = json.optString("transaction_type", "unknown").lowercase()
        val brandName = json.optString("brand_name", "")
        val receiverName = json.optString("receiver_name", "")
        val bank = json.optString("bank", "")
        
        val rawBrand = when (transactionType) {
            "merchant" -> brandName.ifBlank { receiverName }.ifBlank { bank }
            "p2p" -> if (receiverName.isNotBlank()) "$receiverName (โอน)" else bank
            "bill" -> if (receiverName.isNotBlank()) "$receiverName (บิล)" else bank
            else -> receiverName.ifBlank { brandName }.ifBlank { bank }
        }
        
        val confValue = json.optDouble("confidence", 1.0)
        val needsReview = confValue < 0.7
        val confidenceStr = if (needsReview) "low" else "high"

        var parsedTimestamp: Long? = null
        var isDateMissing = false
        var isTimeMissing = false
        
        var dateStr = json.optString("date", "")
        if (dateStr.isNotBlank() && dateStr != "null") {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val year = parts[0].toIntOrNull() ?: 0
                if (year > 2500) {
                    val newYear = year - 543
                    dateStr = "$newYear-${parts[1]}-${parts[2]}"
                }
            }
        }
        var timeStr = json.optString("time", "")

        if (dateStr.isNotBlank() && dateStr != "null") {
            try {
                var localDate = java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                if (timeStr == "24:00") {
                    localDate = localDate.plusDays(1)
                    timeStr = "00:00"
                }
                
                if (timeStr.isNotBlank() && timeStr != "null") {
                    val localTime = java.time.LocalTime.parse(timeStr, java.time.format.DateTimeFormatter.ISO_LOCAL_TIME)
                    parsedTimestamp = java.time.ZonedDateTime.of(localDate, localTime, com.example.zerotouchbudget.domain.util.DateUtils.bangkokZone)
                        .toInstant().toEpochMilli()
                } else {
                    parsedTimestamp = localDate.atStartOfDay(com.example.zerotouchbudget.domain.util.DateUtils.bangkokZone)
                        .toInstant().toEpochMilli()
                    isTimeMissing = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse date/time using java.time: $dateStr $timeStr", e)
                isDateMissing = true
            }
        } else {
            isDateMissing = true
        }

        return ReceiptExtractionData(
            amount = amount,
            brand = normalizeBrand(rawBrand),
            confidence = confidenceStr,
            parserMode = parserMode,
            needsReview = needsReview,
            timestamp = parsedTimestamp,
            isDateMissing = isDateMissing,
            isTimeMissing = isTimeMissing
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
        val parserMode: String,
        val needsReview: Boolean = false,
        val timestamp: Long? = null,
        val isDateMissing: Boolean = false,
        val isTimeMissing: Boolean = false
    )

    companion object {
        private const val TAG = "ProcessReceiptOCR"
        private const val MAX_IMAGE_WIDTH = 1536
        private const val TIMEOUT_MS = 30_000L
    }
}
