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
    private val ocrPrechecker: ReceiptOcrPrechecker,
    private val processedReceiptImageRepository: ProcessedReceiptImageRepository,
    private val heuristics: ReceiptAutoScanHeuristics,
    private val processReceiptImageUseCase: ProcessReceiptImageUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = supervisorScope {
        try {
            val settings = autoScanSettingsRepository.getCurrentSettings()
            if (!settings.enabled) return@supervisorScope Result.success()

            val learnedFolderCounts = processedReceiptImageRepository.getFolderCounts()
            val queriedCandidates = imageFinder.findCandidates(settings.lastScannedAtMillis)
            val candidates = queriedCandidates
                .sortedWith(
                    compareByDescending<ReceiptMediaCandidate> {
                        heuristics.scoreCandidate(
                            relativePath = it.relativePath,
                            displayName = it.displayName,
                            learnedFolderCounts = learnedFolderCounts
                        )
                    }.thenByDescending { it.dateAddedMillis }
                )
                .filterNot { candidate ->
                    processedReceiptImageRepository.isProcessed(candidate.uri)
                }
                .take(MAX_IMAGES_PER_RUN)

            if (candidates.isEmpty()) return@supervisorScope Result.success()

            processInBatches(candidates)

            val newestTimestamp = queriedCandidates.maxOfOrNull { it.dateAddedMillis }
            if (newestTimestamp != null && newestTimestamp > settings.lastScannedAtMillis) {
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

        val ocrPassed = ocrPrechecker.containsReceiptText(bitmap)
        if (!ocrPassed) {
            processedReceiptImageRepository.markProcessed(candidate.toProcessedReceiptImage(false, false))
            return
        }

        val result = processReceiptImageUseCase(bitmap)
        processedReceiptImageRepository.markProcessed(
            candidate.toProcessedReceiptImage(
                ocrPassed = true,
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

    private companion object {
        const val MAX_CONCURRENCY = 2
        const val BATCH_SIZE = 20
        const val BATCH_DELAY_MS = 350L
        const val MAX_BITMAP_EDGE = 1024
        const val MAX_IMAGES_PER_RUN = 20
    }
}
