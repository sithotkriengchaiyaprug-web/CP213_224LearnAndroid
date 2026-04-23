package com.example.zerotouchbudget.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.glance.appwidget.updateAll
import com.example.zerotouchbudget.domain.model.Transaction
import com.example.zerotouchbudget.domain.model.TransactionSource
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import com.example.zerotouchbudget.presentation.widget.BudgetWidget
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
<<<<<<< ours
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
=======
import org.json.JSONException
>>>>>>> theirs
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

class ProcessReceiptImageUseCase @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val repository: TransactionRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(imageUri: Uri): Result<Transaction> = runCatching {
        val bitmap = uriToBitmap(imageUri)
            ?: throw IllegalArgumentException("Unable to decode image from uri: $imageUri")
        val preparedBitmap = prepareImageForApi(bitmap)
        invoke(preparedBitmap).getOrThrow()
    }

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

<<<<<<< ours
        val rawResponse = response.text ?: throw Exception("Empty response from Gemini")
        val parsed = parseReceiptResponse(rawResponse)
        if (parsed.amount <= 0.0) throw IllegalArgumentException("Invalid amount extracted")

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = parsed.amount,
            brand = parsed.brand,
=======
        val responseText = response.text ?: throw Exception("Empty response from Gemini")
        val jsonObject = extractStructuredResponse(responseText)
        val amount = jsonObject.optDouble("amount", -1.0)
        val brand = jsonObject.optString("brand").trim()

        if (amount <= 0.0) {
            throw IllegalArgumentException("Invalid OCR amount: $amount")
        }
        if (brand.isEmpty()) {
            throw IllegalArgumentException("Invalid OCR brand: empty")
        }

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            brand = brand,
>>>>>>> theirs
            category = "Uncategorized",
            timestamp = System.currentTimeMillis(),
            source = TransactionSource.OCR,
            note = "Scanned from receipt"
        )

        repository.insertTransaction(transaction)
        BudgetWidget().updateAll(context)
        transaction
    }

<<<<<<< ours
    private suspend fun uriToBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val decodedBitmap = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        }.getOrNull() ?: return@withContext null

        applyExifRotation(uri, decodedBitmap)
    }

    private suspend fun prepareImageForApi(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        resizeLongestEdge(bitmap, maxLongestEdge = 1536)
    }

    private fun resizeLongestEdge(bitmap: Bitmap, maxLongestEdge: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longestEdge = maxOf(width, height)

        if (longestEdge <= maxLongestEdge) return bitmap

        val scale = maxLongestEdge.toFloat() / longestEdge.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> preScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    preScale(-1f, 1f)
                    postRotate(270f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    preScale(-1f, 1f)
                    postRotate(90f)
                }
            }
        }

        if (matrix.isIdentity) return bitmap

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun parseReceiptResponse(responseText: String): ParsedReceipt {
        val normalized = responseText.trim()
        val jsonCandidate = extractJsonObject(normalized)

        if (jsonCandidate != null) {
            runCatching {
                val json = JSONObject(jsonCandidate)
                val amount = parseAmount(json.optString("amount", ""))
                    ?: json.optDouble("amount").takeIf { !it.isNaN() }
                val brand = json.optString("brand", "").trim()
                if (amount != null && amount > 0.0 && brand.isNotBlank()) {
                    return ParsedReceipt(amount = amount, brand = brand)
                }
            }
        }

        val fallbackAmount = extractAmountFromText(normalized)
            ?: throw IllegalArgumentException("Could not extract amount from Gemini response")
        val fallbackBrand = extractBrandFromText(normalized)
        return ParsedReceipt(amount = fallbackAmount, brand = fallbackBrand)
    }

    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else null
    }

    private fun extractAmountFromText(text: String): Double? {
        val regex = Regex("""(?i)(?:amount|total)\D*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
        return regex.find(text)?.groupValues?.getOrNull(1)?.let(::parseAmount)
    }

    private fun extractBrandFromText(text: String): String {
        val regex = Regex("""(?i)(?:brand|store)\D*([A-Za-z0-9 &\-'.]{2,})""")
        return regex.find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty().ifBlank { "Unknown Merchant" }
    }

    private fun parseAmount(raw: String): Double? {
        val cleaned = raw
            .replace(",", "")
            .replace(Regex("[^0-9.]"), "")
            .trim()
        return cleaned.toDoubleOrNull()
    }

    private data class ParsedReceipt(
        val amount: Double,
        val brand: String
    )
=======
    private fun extractStructuredResponse(responseText: String): JSONObject {
        return parseJsonObject(responseText)
            ?: parseJsonObject(
                Regex("\\{[\\s\\S]*?}")
                    .find(responseText)
                    ?.value
            )
            ?: buildJsonFromFallbackRegex(responseText)
            ?: throw IllegalArgumentException("Gemini response is not valid JSON: $responseText")
    }

    private fun parseJsonObject(raw: String?): JSONObject? {
        if (raw.isNullOrBlank()) return null
        return try {
            JSONObject(raw.trim())
        } catch (_: JSONException) {
            null
        }
    }

    private fun buildJsonFromFallbackRegex(text: String): JSONObject? {
        val amountMatch = Regex("([0-9]+(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)").find(text)
        val brandMatch = Regex("(?i)(?:brand|store|merchant)\\s*[:\\-]\\s*([A-Za-z0-9 .&'-]+)").find(text)

        val amount = amountMatch?.groupValues?.getOrNull(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null

        val brand = brandMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (brand.isBlank()) return null

        return JSONObject().apply {
            put("amount", amount)
            put("brand", brand)
        }
    }
>>>>>>> theirs
}
