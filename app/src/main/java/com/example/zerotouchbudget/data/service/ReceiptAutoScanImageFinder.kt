package com.example.zerotouchbudget.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.example.zerotouchbudget.domain.model.AutoScanSettings
import com.example.zerotouchbudget.domain.model.AutoScanSource
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
        settings: AutoScanSettings,
        maxRowsToInspect: Int = 200
    ): List<ReceiptMediaCandidate> {
        return when (settings.source) {
            AutoScanSource.SCREENSHOTS -> findMediaStoreCandidates(
                settings = settings,
                maxRowsToInspect = maxRowsToInspect
            )
            AutoScanSource.CAMERA -> findMediaStoreCandidates(
                settings = settings,
                maxRowsToInspect = maxRowsToInspect
            )
            AutoScanSource.CUSTOM_FOLDER -> findCustomFolderCandidates(
                settings = settings,
                maxRowsToInspect = maxRowsToInspect
            )
        }
    }

    suspend fun findCandidates(
        lastScannedAtMillis: Long,
        maxRowsToInspect: Int = 200
    ): List<ReceiptMediaCandidate> = findCandidates(
        AutoScanSettings(lastScannedAtMillis = lastScannedAtMillis),
        maxRowsToInspect
    )

    suspend fun findLatestReceipt(lastScannedAtMillis: Long): ReceiptMediaCandidate? {
        return findCandidates(lastScannedAtMillis).firstOrNull()
    }

    private suspend fun findMediaStoreCandidates(
        settings: AutoScanSettings,
        maxRowsToInspect: Int
    ): List<ReceiptMediaCandidate> = withContext(Dispatchers.IO) {
        if (!hasImagePermission()) return@withContext emptyList()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Images.Media.DATE_ADDED} > ?"
        val selectionArgs = arrayOf((settings.lastScannedAtMillis / 1000L).coerceAtLeast(0L).toString())
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
            val mimeTypeIndex = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)

            var inspectedRows = 0
            while (cursor.moveToNext() && inspectedRows < maxRowsToInspect) {
                inspectedRows++

                val displayName = cursor.getString(displayNameIndex).orEmpty()
                val relativePath = if (relativePathIndex >= 0) cursor.getString(relativePathIndex) else null
                val bucketDisplayName = if (bucketDisplayNameIndex >= 0) cursor.getString(bucketDisplayNameIndex) else null
                val mimeType = if (mimeTypeIndex >= 0) cursor.getString(mimeTypeIndex) else null
                val dateAddedMillis = cursor.getLong(dateAddedIndex) * 1000L

                if (dateAddedMillis <= settings.lastScannedAtMillis) continue
                if (!looksLikeSupportedImage(displayName, mimeType)) continue

                val id = cursor.getLong(idIndex)
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                val resolvedRelativePath = relativePath ?: bucketDisplayName?.let { "Pictures/$it/" }

                results += ReceiptMediaCandidate(
                    uri = uri.toString(),
                    displayName = displayName,
                    relativePath = resolvedRelativePath.orEmpty(),
                    dateAddedMillis = dateAddedMillis,
                    folderName = heuristics.extractFolderName(resolvedRelativePath),
                    sourceHintScore = scoreSourceHint(
                        source = settings.source,
                        relativePath = resolvedRelativePath,
                        displayName = displayName,
                        bucketDisplayName = bucketDisplayName
                    )
                )
            }
        }

        results
    }

    private suspend fun findCustomFolderCandidates(
        settings: AutoScanSettings,
        maxRowsToInspect: Int
    ): List<ReceiptMediaCandidate> = withContext(Dispatchers.IO) {
        val folderUriString = settings.customFolderUri.orEmpty()
        if (folderUriString.isBlank()) return@withContext emptyList()

        val rootUri = runCatching { Uri.parse(folderUriString) }.getOrNull()
            ?: return@withContext emptyList()

        val rootDocument = DocumentFile.fromTreeUri(context, rootUri)
            ?: return@withContext emptyList()

        val results = ArrayList<ReceiptMediaCandidate>()
        collectImageCandidatesFromDocumentTree(
            document = rootDocument,
            parentPath = rootDocument.name.orEmpty(),
            lastScannedAtMillis = settings.lastScannedAtMillis,
            results = results
        )

        results
            .sortedByDescending { it.dateAddedMillis }
            .take(maxRowsToInspect)
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

    private fun looksLikeSupportedImage(displayName: String, mimeType: String?): Boolean {
        if (mimeType?.startsWith("image/") == true) return true
        val normalized = displayName.lowercase()
        return imageFileExtensions.any { normalized.endsWith(it) }
    }

    private fun scoreSourceHint(
        source: AutoScanSource,
        relativePath: String?,
        displayName: String,
        bucketDisplayName: String?
    ): Int {
        val normalized = listOfNotNull(relativePath, displayName, bucketDisplayName)
            .joinToString(" ")
            .lowercase()

        return when (source) {
            AutoScanSource.SCREENSHOTS -> if (listOf(
                    "screenshot",
                    "screen shot",
                    "screen_capture",
                    "screen capture",
                    "screenshots"
                ).any { normalized.contains(it) }
            ) 40 else 0

            AutoScanSource.CAMERA -> if (listOf(
                    "camera",
                    "dcim",
                    "img_",
                    "dsc_",
                    "photo"
                ).any { normalized.contains(it) }
            ) 40 else 0

            AutoScanSource.CUSTOM_FOLDER -> 0
        }
    }

    private fun collectImageCandidatesFromDocumentTree(
        document: DocumentFile,
        parentPath: String,
        lastScannedAtMillis: Long,
        results: MutableList<ReceiptMediaCandidate>
    ) {
        if (document.isFile) {
            val displayName = document.name.orEmpty()
            if (!looksLikeSupportedImage(displayName, document.type)) return

            val modifiedAt = document.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
            if (modifiedAt <= lastScannedAtMillis) return

            results += ReceiptMediaCandidate(
                uri = document.uri.toString(),
                displayName = displayName,
                relativePath = parentPath,
                dateAddedMillis = modifiedAt,
                folderName = heuristics.extractFolderName(parentPath),
                sourceHintScore = if (heuristics.isLikelyReceiptFile(displayName)) 20 else 0
            )
            return
        }

        val nextParent = document.name?.takeIf { it.isNotBlank() }?.let { name ->
            if (parentPath.isBlank()) name else "$parentPath/$name"
        } ?: parentPath

        document.listFiles().forEach { child ->
            val childPath = if (child.isFile) {
                nextParent
            } else {
                child.name?.takeIf { it.isNotBlank() }?.let { name ->
                    if (nextParent.isBlank()) name else "$nextParent/$name"
                } ?: nextParent
            }
            collectImageCandidatesFromDocumentTree(
                document = child,
                parentPath = childPath,
                lastScannedAtMillis = lastScannedAtMillis,
                results = results
            )
        }
    }

    private val imageFileExtensions = listOf(
        ".jpg",
        ".jpeg",
        ".png",
        ".webp",
        ".heic",
        ".heif"
    )
}
