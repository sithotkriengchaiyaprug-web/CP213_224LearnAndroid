package com.example.zerotouchbudget.data.service

import android.graphics.Bitmap
import com.example.zerotouchbudget.domain.model.ReceiptAutoScanHeuristics
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptOcrPrechecker @Inject constructor(
    private val textRecognizer: TextRecognizer,
    private val heuristics: ReceiptAutoScanHeuristics
) {

    suspend fun containsReceiptText(bitmap: Bitmap): Boolean = withContext(Dispatchers.Default) {
        val result = textRecognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        heuristics.containsReceiptKeywords(result.text)
    }
}
