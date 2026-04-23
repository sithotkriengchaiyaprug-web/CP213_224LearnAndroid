package com.example.zerotouchbudget.domain.model

data class ReceiptMediaCandidate(
    val uri: String,
    val displayName: String,
    val relativePath: String,
    val dateAddedMillis: Long,
    val folderName: String
)

