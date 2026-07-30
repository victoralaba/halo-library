package com.example.data.parser

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.data.repository.AudioRepository
import com.example.data.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class ScanResult(
    val booksAdded: Int,
    val audioAdded: Int
)

object StorageScanner {
    private const val TAG = "StorageScanner"

    private val BOOK_EXTENSIONS = setOf("epub", "pdf")
    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "m4b", "aac", "wav", "ogg", "flac")

    suspend fun scanFolders(
        context: Context,
        folderUris: List<String>,
        bookRepository: BookRepository,
        audioRepository: AudioRepository
    ): ScanResult = withContext(Dispatchers.IO) {
        var booksAdded = 0
        var audioAdded = 0

        val existingBooks = bookRepository.allBooks.first()
        val existingBookPaths = existingBooks.map { it.filePath }.toSet()
        val existingBookTitles = existingBooks.map { it.title.lowercase() }.toSet()

        val existingTracks = audioRepository.allTracks.first()
        val existingTrackPaths = existingTracks.map { it.filePath }.toSet()
        val existingTrackTitles = existingTracks.map { it.title.lowercase() }.toSet()

        for (uriString in folderUris) {
            try {
                val uri = Uri.parse(uriString)
                val tree = DocumentFile.fromTreeUri(context, uri) ?: continue
                val foundFiles = mutableListOf<DocumentFile>()
                collectFiles(tree, foundFiles)

                for (doc in foundFiles) {
                    val name = doc.name ?: continue
                    val ext = name.substringAfterLast(".").lowercase()
                    val docUriStr = doc.uri.toString()

                    if (ext in BOOK_EXTENSIONS) {
                        val simpleName = name.substringBeforeLast(".").lowercase()
                        if (docUriStr !in existingBookPaths && simpleName !in existingBookTitles) {
                            try {
                                bookRepository.importBookFromUri(doc.uri, name.substringBeforeLast("."))
                                booksAdded++
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed importing scanned book ${doc.uri}", e)
                            }
                        }
                    } else if (ext in AUDIO_EXTENSIONS) {
                        val simpleName = name.substringBeforeLast(".").lowercase()
                        if (docUriStr !in existingTrackPaths && simpleName !in existingTrackTitles) {
                            try {
                                audioRepository.importAudioUri(doc.uri)
                                audioAdded++
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed importing scanned audio track ${doc.uri}", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning directory $uriString", e)
            }
        }

        ScanResult(booksAdded = booksAdded, audioAdded = audioAdded)
    }

    private fun collectFiles(dir: DocumentFile, resultList: MutableList<DocumentFile>, maxDepth: Int = 3, currentDepth: Int = 0) {
        if (currentDepth > maxDepth || !dir.exists() || !dir.isDirectory) return
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                collectFiles(file, resultList, maxDepth, currentDepth + 1)
            } else if (file.isFile) {
                resultList.add(file)
            }
        }
    }
}
