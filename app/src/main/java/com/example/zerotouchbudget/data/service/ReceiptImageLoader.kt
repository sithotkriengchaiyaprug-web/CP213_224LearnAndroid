package com.example.zerotouchbudget.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptImageLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun loadPreparedBitmap(
        uri: Uri,
        maxLongestEdge: Int = 1024
    ): Bitmap? = withContext(Dispatchers.IO) {
        val decodedBitmap = decodeBitmap(uri) ?: return@withContext null
        val rotatedBitmap = applyExifRotation(uri, decodedBitmap)
        resizeLongestEdge(rotatedBitmap, maxLongestEdge)
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        }.getOrNull()
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
}

