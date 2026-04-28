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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.example.zerotouchbudget.domain.model.Transaction
import com.example.zerotouchbudget.domain.model.TransactionSource
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import com.example.zerotouchbudget.presentation.widget.BudgetWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class ProcessReceiptImageUseCase @Inject constructor(
    private val textRecognizer: TextRecognizer,
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
        val preparedBitmap = prepareImageForApi(bitmap)
        val parsed = analyzeReceiptWithOcr(preparedBitmap)
        if (parsed.amount <= 0.0) throw IllegalArgumentException("Invalid amount extracted")

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = parsed.amount,
            brand = parsed.brand,
            category = "Uncategorized",
            timestamp = System.currentTimeMillis(),
            source = TransactionSource.OCR,
            note = "Scanned from receipt"
        )

        repository.insertTransaction(transaction)
        BudgetWidget().updateAll(context)
        transaction
    }

    private suspend fun analyzeReceiptWithOcr(
        bitmap: Bitmap
    ): ParsedReceipt {
        val result = textRecognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        val rawText = result.text.trim()
        if (rawText.isBlank()) {
            throw IllegalArgumentException("Empty OCR result from ML Kit")
        }
        return parseReceiptText(rawText)
    }

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
        resizeLongestEdge(bitmap, maxLongestEdge = 1024)
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

    private fun parseReceiptText(responseText: String): ParsedReceipt {
        val normalized = responseText.trim()
        val fallbackAmount = extractAmountFromText(normalized)
            ?: throw IllegalArgumentException("Could not extract amount from OCR result")
        val fallbackBrand = extractBrandFromText(normalized)
        return ParsedReceipt(amount = fallbackAmount, brand = fallbackBrand)
    }

    private fun extractAmountFromText(text: String): Double? {
        val labeledPatterns = listOf(
            Regex("""(?i)(?:amount|total|grand total|net|balance|sum|\u0E22\u0E2D\u0E14\u0E23\u0E27\u0E21|\u0E23\u0E27\u0E21\u0E17\u0E31\u0E49\u0E07\u0E2A\u0E34\u0E49\u0E19|\u0E23\u0E27\u0E21|\u0E0A\u0E33\u0E23\u0E30\u0E40\u0E07\u0E34\u0E19|\u0E08\u0E33\u0E19\u0E27\u0E19\u0E40\u0E07\u0E34\u0E19)\D*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""),
            Regex("""(?i)([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(?:\u0E1A\u0E32\u0E17|thb|\u0E3F)""")
        )

        for (pattern in labeledPatterns) {
            val value = pattern.find(text)?.groupValues?.getOrNull(1)?.let(::parseAmount)
            if (value != null) return value
        }

        return Regex("""([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
            .findAll(text)
            .mapNotNull { match -> parseAmount(match.groupValues.getOrNull(1).orEmpty()) }
            .maxOrNull()
    }

    private fun extractBrandFromText(text: String): String {
        val lines = text
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        val blockedWords = listOf(
            "receipt",
            "invoice",
            "total",
            "subtotal",
            "amount",
            "change",
            "cash",
            "vat",
            "thank you"
        )

        return lines.firstOrNull { line ->
            line.length in 2..40 &&
                line.any { it.isLetter() } &&
                !line.any { it.isDigit() } &&
                blockedWords.none { word -> line.contains(word, ignoreCase = true) }
        }?.trim().orEmpty().ifBlank { "Unknown Merchant" }
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
}
