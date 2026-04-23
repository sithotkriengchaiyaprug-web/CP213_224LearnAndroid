package com.example.zerotouchbudget.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.zerotouchbudget.domain.model.ReceiptAutoScanHeuristics
import com.example.zerotouchbudget.domain.model.ReceiptMediaCandidate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptAutoScanImageFinder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val heuristics: ReceiptAutoScanHeuristics
) {

    suspend fun findCandidates(
        lastScannedAtMillis: Long,
        maxRowsToInspect: Int = 200
    ): List<ReceiptMediaCandidate> = withContext(Dispatchers.IO) {
        if (!hasImagePermission()) return@withContext emptyList()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val selection = buildSelection()
        val selectionArgs = buildSelectionArgs(lastScannedAtMillis)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val results = ArrayList<ReceiptMediaCandidate>()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val relativePathIndex = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val bucketDisplayNameIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            var inspectedRows = 0
            while (cursor.moveToNext() && inspectedRows < maxRowsToInspect) {
                inspectedRows++

                val displayName = cursor.getString(displayNameIndex).orEmpty()
                val relativePath = if (relativePathIndex >= 0) {
                    cursor.getString(relativePathIndex)
                } else {
                    null
                }
                val bucketDisplayName = if (bucketDisplayNameIndex >= 0) {
                    cursor.getString(bucketDisplayNameIndex)
                } else {
                    null
                }
                val dateAddedSeconds = cursor.getLong(dateAddedIndex)
                val dateAddedMillis = dateAddedSeconds * 1000L

                if (dateAddedMillis <= lastScannedAtMillis) continue
                val resolvedRelativePath = relativePath
                    ?: bucketDisplayName?.let { "Pictures/$it/" }
                if (!heuristics.isBankFolder(resolvedRelativePath)) continue
                if (!heuristics.isLikelyReceiptFile(displayName)) continue

                val id = cursor.getLong(idIndex)
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                results += ReceiptMediaCandidate(
                    uri = uri.toString(),
                    displayName = displayName,
                    relativePath = resolvedRelativePath.orEmpty(),
                    dateAddedMillis = dateAddedMillis,
                    folderName = heuristics.extractFolderName(resolvedRelativePath)
                )
            }
        }

        results
    }

    suspend fun findLatestReceipt(lastScannedAtMillis: Long): ReceiptMediaCandidate? {
        return findCandidates(lastScannedAtMillis).firstOrNull()
    }

    private fun buildSelection(): String {
        val receiptNameClauses = listOf("slip", "receipt", "transfer", "payment")
            .joinToString(" OR ") { "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?" }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bankFolderClauses = listOf("scb", "kbank", "krungthai", "bbl", "ktb")
                .joinToString(" OR ") { "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?" }
            buildString {
                append("${MediaStore.Images.Media.DATE_ADDED} > ?")
                append(" AND ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?")
                append(" AND (")
                append(bankFolderClauses)
                append(")")
                append(" AND (")
                append(receiptNameClauses)
                append(")")
            }
        } else {
            val bankFolderClauses = listOf("scb", "kbank", "krungthai", "bbl", "ktb")
                .joinToString(" OR ") { "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} LIKE ?" }
            buildString {
                append("${MediaStore.Images.Media.DATE_ADDED} > ?")
                append(" AND (")
                append(bankFolderClauses)
                append(")")
                append(" AND (")
                append(receiptNameClauses)
                append(")")
            }
        }
    }

    private fun buildSelectionArgs(lastScannedAtMillis: Long): Array<String> {
        val lastScannedAtSeconds = (lastScannedAtMillis / 1000L).coerceAtLeast(0L).toString()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                lastScannedAtSeconds,
                "Pictures/%",
                "%scb%",
                "%kbank%",
                "%krungthai%",
                "%bbl%",
                "%ktb%",
                "%slip%",
                "%receipt%",
                "%transfer%",
                "%payment%"
            )
        } else {
            arrayOf(
                lastScannedAtSeconds,
                "%scb%",
                "%kbank%",
                "%krungthai%",
                "%bbl%",
                "%ktb%",
                "%slip%",
                "%receipt%",
                "%transfer%",
                "%payment%"
            )
        }
    }

    private fun hasImagePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
