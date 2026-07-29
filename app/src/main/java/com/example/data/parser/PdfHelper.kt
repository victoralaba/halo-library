package com.example.data.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream

data class PdfPageInfo(
    val pageIndex: Int,
    val pageCount: Int,
    val width: Int,
    val height: Int
)

object PdfHelper {
    private const val TAG = "PdfHelper"

    // 32MB Memory LRU Cache for rendered PDF bitmaps
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceIn(16 * 1024, 64 * 1024) // 16MB - 64MB

    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    // Active session holder to avoid re-opening file descriptors and native PdfRenderers continuously
    private class PdfRendererSession(
        val pfd: ParcelFileDescriptor,
        val renderer: PdfRenderer,
        val key: String
    )

    @Volatile
    private var activeSession: PdfRendererSession? = null

    private fun getOrCreateSession(
        context: Context,
        fileUri: Uri,
        isAsset: Boolean,
        assetName: String?
    ): PdfRendererSession? {
        val sessionKey = if (isAsset && !assetName.isNullOrEmpty()) assetName else fileUri.toString()

        synchronized(this) {
            val current = activeSession
            if (current != null && current.key == sessionKey) {
                return current
            }

            // Close previous session if different document
            current?.let {
                try {
                    it.renderer.close()
                    it.pfd.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing previous PDF session", e)
                }
            }

            return try {
                val pfd = getFileDescriptor(context, fileUri, isAsset, assetName) ?: return null
                val renderer = PdfRenderer(pfd)
                val newSession = PdfRendererSession(pfd, renderer, sessionKey)
                activeSession = newSession
                newSession
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing PDF session for $sessionKey", e)
                null
            }
        }
    }

    fun getPageCount(context: Context, fileUri: Uri, isAsset: Boolean, assetName: String?): Int {
        val session = getOrCreateSession(context, fileUri, isAsset, assetName) ?: return 0
        return synchronized(session) {
            try {
                session.renderer.pageCount
            } catch (e: Exception) {
                Log.e(TAG, "Error getting PDF page count", e)
                0
            }
        }
    }

    fun renderPageBitmap(
        context: Context,
        fileUri: Uri,
        isAsset: Boolean,
        assetName: String?,
        pageIndex: Int,
        targetWidth: Int = 1080
    ): Bitmap? {
        val sessionKey = if (isAsset && !assetName.isNullOrEmpty()) assetName else fileUri.toString()
        val cacheKey = "${sessionKey}_${pageIndex}_$targetWidth"

        // 1. Fast path: return cached bitmap instantly if present
        bitmapCache.get(cacheKey)?.let { cachedBitmap ->
            if (!cachedBitmap.isRecycled) return cachedBitmap
        }

        // 2. Render bitmap using cached active PdfRenderer session
        val session = getOrCreateSession(context, fileUri, isAsset, assetName) ?: return null

        return synchronized(session) {
            try {
                if (pageIndex < 0 || pageIndex >= session.renderer.pageCount) return null

                session.renderer.openPage(pageIndex).use { page ->
                    val aspectRatio = page.height.toFloat() / page.width.toFloat()
                    val calcHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(100)

                    val bitmap = Bitmap.createBitmap(targetWidth, calcHeight, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    bitmapCache.put(cacheKey, bitmap)
                    bitmap
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rendering PDF page $pageIndex", e)
                null
            }
        }
    }

    fun clearCache() {
        synchronized(this) {
            bitmapCache.evictAll()
            activeSession?.let {
                try {
                    it.renderer.close()
                    it.pfd.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing PDF session on clearCache", e)
                }
            }
            activeSession = null
        }
    }

    private fun getFileDescriptor(
        context: Context,
        fileUri: Uri,
        isAsset: Boolean,
        assetName: String?
    ): ParcelFileDescriptor? {
        return if (isAsset && !assetName.isNullOrEmpty()) {
            val file = File(context.cacheDir, "temp_pdf_${assetName.hashCode()}.pdf")
            if (!file.exists()) {
                context.assets.open(assetName).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } else {
            try {
                context.contentResolver.openFileDescriptor(fileUri, "r")
            } catch (e: Exception) {
                val file = File(fileUri.path ?: "")
                if (file.exists()) {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                } else null
            }
        }
    }
}

