package com.example.data.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
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

    fun getPageCount(context: Context, fileUri: Uri, isAsset: Boolean, assetName: String?): Int {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = getFileDescriptor(context, fileUri, isAsset, assetName)
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                renderer.pageCount
            } else 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting PDF page count", e)
            0
        } finally {
            renderer?.close()
            pfd?.close()
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
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = getFileDescriptor(context, fileUri, isAsset, assetName) ?: return null
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null

            page = renderer.openPage(pageIndex)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val calcHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(100)

            val bitmap = Bitmap.createBitmap(targetWidth, calcHeight, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering PDF page $pageIndex", e)
            null
        } finally {
            page?.close()
            renderer?.close()
            pfd?.close()
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
