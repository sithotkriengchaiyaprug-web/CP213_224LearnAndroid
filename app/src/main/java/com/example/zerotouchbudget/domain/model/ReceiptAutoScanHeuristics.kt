package com.example.zerotouchbudget.domain.model

import javax.inject.Inject

class ReceiptAutoScanHeuristics @Inject constructor() {

    private val bankKeywords = listOf("scb", "kbank", "krungthai", "bbl", "ktb")
    private val filenameKeywords = listOf("slip", "receipt", "transfer", "payment")
    private val ocrKeywords = listOf(
        "\u0e08\u0e33\u0e19\u0e27\u0e19\u0e40\u0e07\u0e34\u0e19", // จำนวนเงิน
        "amount",
        "\u0e1a\u0e32\u0e17", // บาท
        "\u0e42\u0e2d\u0e19\u0e40\u0e07\u0e34\u0e19", // โอนเงิน
        "\u0e0a\u0e33\u0e23\u0e30\u0e40\u0e07\u0e34\u0e19" // ชำระเงิน
    )

    fun isPicturesPath(relativePath: String?): Boolean {
        return normalize(relativePath).startsWith("pictures/")
    }

    fun isBankFolder(relativePath: String?): Boolean {
        val normalized = normalize(relativePath)
        return isPicturesPath(normalized) && bankKeywords.any { normalized.contains(it) }
    }

    fun isLikelyReceiptFile(displayName: String?): Boolean {
        val normalized = normalize(displayName)
        return filenameKeywords.any { normalized.contains(it) }
    }

    fun containsReceiptKeywords(text: String?): Boolean {
        val normalized = normalize(text)
        return ocrKeywords.any { normalized.contains(it) }
    }

    fun extractFolderName(relativePath: String?): String {
        val normalized = normalize(relativePath).trim('/')
        if (normalized.isBlank()) return "unknown"
        return normalized.split('/')
            .lastOrNull { it.isNotBlank() }
            ?.trim()
            .orEmpty()
            .ifBlank { "unknown" }
    }

    fun folderPriority(folderName: String, learnedCounts: Map<String, Int>): Int {
        val normalized = folderName.trim().lowercase()
        val learnedScore = learnedCounts[folderName] ?: learnedCounts[normalized] ?: 0
        val bankScore = if (bankKeywords.any { normalized.contains(it) }) 100 else 0
        return bankScore + learnedScore
    }

    fun scoreCandidate(
        relativePath: String?,
        displayName: String?,
        ocrText: String? = null,
        learnedFolderCounts: Map<String, Int> = emptyMap()
    ): Int {
        var score = folderPriority(extractFolderName(relativePath), learnedFolderCounts)
        if (isBankFolder(relativePath)) {
            score += 25
        }
        if (isLikelyReceiptFile(displayName)) {
            score += 25
        }
        if (containsReceiptKeywords(ocrText)) {
            score += 50
        }
        return score
    }

    fun selectPrimaryFolder(folderCounts: Map<String, Int>): String? {
        return folderCounts.maxByOrNull { it.value }?.key
    }

    fun isPotentialReceipt(relativePath: String?, displayName: String?): Boolean {
        return isBankFolder(relativePath) && isLikelyReceiptFile(displayName)
    }

    private fun normalize(value: String?): String {
        return value.orEmpty().lowercase()
    }
}
