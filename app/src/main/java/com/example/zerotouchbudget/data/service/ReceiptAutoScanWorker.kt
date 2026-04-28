package com.example.zerotouchbudget.data.service

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.zerotouchbudget.domain.model.ProcessedReceiptImage
import com.example.zerotouchbudget.domain.model.ReceiptAutoScanHeuristics
import com.example.zerotouchbudget.domain.model.ReceiptMediaCandidate
import com.example.zerotouchbudget.domain.repository.AutoScanSettingsRepository
import com.example.zerotouchbudget.domain.repository.ProcessedReceiptImageRepository
import com.example.zerotouchbudget.domain.usecase.ProcessReceiptImageUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@HiltWorker
class ReceiptAutoScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val autoScanSettingsRepository: AutoScanSettingsRepository,
    private val imageFinder: ReceiptAutoScanImageFinder,
    private val imageLoader: ReceiptImageLoader,
    private val processedReceiptImageRepository: ProcessedReceiptImageRepository,
    private val heuristics: ReceiptAutoScanHeuristics,
    private val processReceiptImageUseCase: ProcessReceiptImageUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = supervisorScope {
        try {
            val settings = autoScanSettingsRepository.getCurrentSettings()
            val allowWhenDisabled = inputData.getBoolean(KEY_ALLOW_DISABLED, false)
            val syncHistory = inputData.getBoolean(KEY_SYNC_HISTORY, false)
            if (!settings.enabled && !allowWhenDisabled) return@supervisorScope Result.success()
            if (settings.source == com.example.zerotouchbudget.domain.model.AutoScanSource.CUSTOM_FOLDER &&
                settings.customFolderUri.isNullOrBlank()
            ) {
                return@supervisorScope Result.success()
            }

            val scanSettings = if (syncHistory) {
                settings.copy(lastScannedAtMillis = 0L)
            } else {
                settings
            }
            val maxRowsToInspect = if (syncHistory) 500 else 200
            val learnedFolderCounts = processedReceiptImageRepository.getFolderCounts()
            val queriedCandidates = imageFinder.findCandidates(
                settings = scanSettings,
                maxRowsToInspect = maxRowsToInspect
            )
            val candidates = queriedCandidates
                .sortedWith(
                    compareByDescending<ReceiptMediaCandidate> {
                        heuristics.scoreCandidate(
                            relativePath = it.relativePath,
                            displayName = it.displayName,
                            learnedFolderCounts = learnedFolderCounts
                        ) + it.sourceHintScore
                    }.thenByDescending { it.dateAddedMillis }
                )
                .filterNot { candidate ->
                    processedReceiptImageRepository.isProcessed(candidate.uri)
                }

            if (candidates.isEmpty()) return@supervisorScope Result.success()

            processInBatches(candidates)

            val newestTimestamp = queriedCandidates.maxOfOrNull { it.dateAddedMillis }
            if (
                newestTimestamp != null &&
                queriedCandidates.size < maxRowsToInspect &&
                newestTimestamp > settings.lastScannedAtMillis
            ) {
                autoScanSettingsRepository.updateLastScannedAt(newestTimestamp)
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun processInBatches(candidates: List<ReceiptMediaCandidate>) = supervisorScope {
        val semaphore = Semaphore(MAX_CONCURRENCY)
        candidates.chunked(BATCH_SIZE).forEachIndexed { batchIndex, batch ->
            batch.map { candidate ->
                async {
                    semaphore.withPermit {
                        processCandidate(candidate)
                    }
                }
            }.awaitAll()

            if (batchIndex < candidates.chunked(BATCH_SIZE).lastIndex) {
                delay(BATCH_DELAY_MS)
            }
        }
    }

    private suspend fun processCandidate(candidate: ReceiptMediaCandidate) {
        if (processedReceiptImageRepository.isProcessed(candidate.uri)) return

        val bitmap = imageLoader.loadPreparedBitmap(
            uri = Uri.parse(candidate.uri),
            maxLongestEdge = MAX_BITMAP_EDGE
        ) ?: run {
            processedReceiptImageRepository.markProcessed(candidate.toProcessedReceiptImage(false, false))
            return
        }

        val result = processReceiptImageUseCase(bitmap)
        processedReceiptImageRepository.markProcessed(
            candidate.toProcessedReceiptImage(
                ocrPassed = result.isSuccess,
                aiProcessed = result.isSuccess
            )
        )
    }

    private fun ReceiptMediaCandidate.toProcessedReceiptImage(
        ocrPassed: Boolean,
        aiProcessed: Boolean
    ): ProcessedReceiptImage {
        return ProcessedReceiptImage(
            imageUri = uri,
            displayName = displayName,
            relativePath = relativePath,
            folderName = folderName,
            processedAtMillis = System.currentTimeMillis(),
            ocrPassed = ocrPassed,
            aiProcessed = aiProcessed
        )
    }

    companion object {
        const val KEY_ALLOW_DISABLED = "allow_when_disabled"
        const val KEY_SYNC_HISTORY = "sync_history"
        const val MAX_CONCURRENCY = 2
        const val BATCH_SIZE = 20
        const val BATCH_DELAY_MS = 350L
        const val MAX_BITMAP_EDGE = 1024
    }
}
