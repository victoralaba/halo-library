package com.example.data.parser

import android.content.Context
import android.net.Uri
import android.util.Log
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

data class EpubChapter(
    val chapterIndex: Int,
    val title: String,
    val paragraphs: List<String>
)

data class ParsedEpub(
    val title: String,
    val author: String,
    val description: String? = null,
    val publisher: String? = null,
    val language: String? = null,
    val coverBytes: ByteArray? = null,
    val chapters: List<EpubChapter>
)

object EpubParser {
    private const val TAG = "EpubParser"

    fun parseEpubFromUri(context: Context, uri: Uri): ParsedEpub? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                parseEpubFromStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening EPUB from Uri: $uri", e)
            null
        }
    }

    fun parseEpubFromAsset(context: Context, assetName: String): ParsedEpub? {
        return try {
            context.assets.open(assetName).use { inputStream ->
                parseEpubFromStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening EPUB from asset: $assetName", e)
            null
        }
    }

    private fun parseEpubFromStream(inputStream: InputStream): ParsedEpub {
        val zipEntries = mutableMapOf<String, ByteArray>()
        val zipStream = ZipInputStream(inputStream)
        var entry = zipStream.nextEntry

        while (entry != null) {
            if (!entry.isDirectory) {
                zipEntries[normalizePath(entry.name)] = zipStream.readBytes()
            }
            zipStream.closeEntry()
            entry = zipStream.nextEntry
        }

        // Step 1: Find OPF path in META-INF/container.xml
        var opfPath = findOpfPath(zipEntries)
        if (opfPath == null) {
            // Fallback: search for any .opf file in zip
            opfPath = zipEntries.keys.firstOrNull { it.endsWith(".opf") }
        }

        if (opfPath == null) {
            Log.w(TAG, "No OPF package found, using fallback HTML parsing")
            return parseFallbackZipHtml(zipEntries)
        }

        val opfBytes = zipEntries[normalizePath(opfPath)]
            ?: return parseFallbackZipHtml(zipEntries)

        val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""

        // Step 2: Parse OPF metadata, manifest, and spine
        val docBuilder = DocumentBuilderFactory.newInstance().also {
            it.isNamespaceAware = false
        }.newDocumentBuilder()

        val opfDoc = try {
            docBuilder.parse(ByteArrayInputStream(opfBytes))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse OPF XML", e)
            return parseFallbackZipHtml(zipEntries)
        }

        var title = "Unknown Title"
        var author = "Unknown Author"
        var description: String? = null
        var publisher: String? = null
        var language: String? = null

        val titleNodes = opfDoc.getElementsByTagName("dc:title")
        if (titleNodes.length > 0) {
            val t = titleNodes.item(0).textContent.trim()
            if (t.isNotBlank()) title = t
        }

        val creatorNodes = opfDoc.getElementsByTagName("dc:creator")
        if (creatorNodes.length > 0) {
            val a = creatorNodes.item(0).textContent.trim()
            if (a.isNotBlank()) author = a
        }

        val descNodes = opfDoc.getElementsByTagName("dc:description")
        if (descNodes.length > 0) {
            val d = descNodes.item(0).textContent.replace(Regex("<[^>]*>"), "").trim()
            if (d.isNotBlank()) description = d
        }

        val pubNodes = opfDoc.getElementsByTagName("dc:publisher")
        if (pubNodes.length > 0) {
            val p = pubNodes.item(0).textContent.trim()
            if (p.isNotBlank()) publisher = p
        }

        val langNodes = opfDoc.getElementsByTagName("dc:language")
        if (langNodes.length > 0) {
            val l = langNodes.item(0).textContent.trim()
            if (l.isNotBlank()) language = l
        }

        // Parse Manifest items
        val manifestMap = mutableMapOf<String, String>() // id -> href
        var ncxHref: String? = null
        var navHref: String? = null

        val itemNodes = opfDoc.getElementsByTagName("item")
        for (i in 0 until itemNodes.length) {
            val item = itemNodes.item(i) as Element
            val id = item.getAttribute("id")
            val href = item.getAttribute("href")
            val mediaType = item.getAttribute("media-type")
            val properties = item.getAttribute("properties")

            if (id.isNotBlank() && href.isNotBlank()) {
                manifestMap[id] = href
            }
            if (mediaType == "application/x-dtbncx+xml" || id.equals("ncx", ignoreCase = true)) {
                ncxHref = href
            }
            if (properties.contains("nav")) {
                navHref = href
            }
        }

        val coverBytes = extractCoverBytesFromZip(zipEntries, opfDoc, opfPath, manifestMap)

        // Parse Spine reading order
        val spineIdRefs = mutableListOf<String>()
        val itemRefNodes = opfDoc.getElementsByTagName("itemref")
        for (i in 0 until itemRefNodes.length) {
            val itemRef = itemRefNodes.item(i) as Element
            val idref = itemRef.getAttribute("idref")
            val linear = itemRef.getAttribute("linear")
            if (idref.isNotBlank() && linear != "no") {
                spineIdRefs.add(idref)
            }
        }

        // Step 3: Parse TOC to map file/anchor to Chapter Titles
        val tocTitleMap = mutableMapOf<String, String>() // normalized path -> title

        // Try parsing NCX
        val resolvedNcxPath = ncxHref?.let { normalizePath(opfDir + it) }
        val ncxBytes = resolvedNcxPath?.let { zipEntries[it] }
        if (ncxBytes != null) {
            parseNcxToc(ncxBytes, opfDir, tocTitleMap)
        }

        // Try parsing Nav XHTML if NCX failed or incomplete
        if (tocTitleMap.isEmpty()) {
            val resolvedNavPath = navHref?.let { normalizePath(opfDir + it) }
            val navBytes = resolvedNavPath?.let { zipEntries[it] }
            if (navBytes != null) {
                parseNavXhtmlToc(navBytes, opfDir, tocTitleMap)
            }
        }

        // Step 4: Extract chapters in Spine order
        val chapters = mutableListOf<EpubChapter>()
        val processedHashes = mutableSetOf<Int>()
        var chapterIndex = 0

        for (idref in spineIdRefs) {
            val relHref = manifestMap[idref] ?: continue
            val fullPath = normalizePath(opfDir + relHref)
            val fileBytes = zipEntries[fullPath] ?: continue

            val rawHtml = String(fileBytes, Charsets.UTF_8)
            val paragraphs = parseHtmlToParagraphs(rawHtml)
            if (paragraphs.isEmpty()) continue

            // Deduplicate identical chapter text content
            val contentHash = paragraphs.joinToString("").hashCode()
            if (processedHashes.contains(contentHash)) continue
            processedHashes.add(contentHash)

            // Chapter Title Resolution
            var chapterTitle = tocTitleMap[fullPath]
                ?: tocTitleMap[fullPath.substringAfterLast("/")]
                ?: extractChapterTitle(rawHtml, fullPath, chapterIndex + 1)

            // Clean title formatting
            chapterTitle = chapterTitle.replace(Regex("\\s+"), " ").trim()

            chapters.add(
                EpubChapter(
                    chapterIndex = chapterIndex,
                    title = chapterTitle,
                    paragraphs = paragraphs
                )
            )
            chapterIndex++
        }

        // Fallback if spine yield no chapters
        if (chapters.isEmpty()) {
            return parseFallbackZipHtml(zipEntries)
        }

        // Filter title if placeholder
        if (title == "Unknown Title" || title.isBlank()) {
            title = chapters.firstOrNull()?.paragraphs?.firstOrNull()?.take(40) ?: "Imported Book"
        }

        return ParsedEpub(
            title = title,
            author = author,
            description = description,
            publisher = publisher,
            language = language,
            coverBytes = coverBytes,
            chapters = chapters
        )
    }

    private fun findOpfPath(zipEntries: Map<String, ByteArray>): String? {
        val containerBytes = zipEntries["meta-inf/container.xml"] ?: return null
        return try {
            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = docBuilder.parse(ByteArrayInputStream(containerBytes))
            val rootfiles = doc.getElementsByTagName("rootfile")
            if (rootfiles.length > 0) {
                (rootfiles.item(0) as Element).getAttribute("full-path")
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseNcxToc(ncxBytes: ByteArray, opfDir: String, titleMap: MutableMap<String, String>) {
        try {
            val docBuilder = DocumentBuilderFactory.newInstance().also {
                it.isNamespaceAware = false
            }.newDocumentBuilder()
            val doc = docBuilder.parse(ByteArrayInputStream(ncxBytes))

            val navPoints = doc.getElementsByTagName("navPoint")
            for (i in 0 until navPoints.length) {
                val np = navPoints.item(i) as Element
                val textNodes = np.getElementsByTagName("text")
                val contentNodes = np.getElementsByTagName("content")

                if (textNodes.length > 0 && contentNodes.length > 0) {
                    val title = textNodes.item(0).textContent.trim()
                    val src = (contentNodes.item(0) as Element).getAttribute("src")
                    if (title.isNotBlank() && src.isNotBlank()) {
                        val cleanSrc = src.substringBefore("#")
                        val fullPath = normalizePath(opfDir + cleanSrc)
                        if (!titleMap.containsKey(fullPath)) {
                            titleMap[fullPath] = title
                            titleMap[cleanSrc.substringAfterLast("/")] = title
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing NCX TOC", e)
        }
    }

    private fun parseNavXhtmlToc(navBytes: ByteArray, opfDir: String, titleMap: MutableMap<String, String>) {
        try {
            val docBuilder = DocumentBuilderFactory.newInstance().also {
                it.isNamespaceAware = false
            }.newDocumentBuilder()
            val doc = docBuilder.parse(ByteArrayInputStream(navBytes))

            val aNodes = doc.getElementsByTagName("a")
            for (i in 0 until aNodes.length) {
                val a = aNodes.item(i) as Element
                val href = a.getAttribute("href")
                val title = a.textContent.trim()
                if (title.isNotBlank() && href.isNotBlank()) {
                    val cleanSrc = href.substringBefore("#")
                    val fullPath = normalizePath(opfDir + cleanSrc)
                    if (!titleMap.containsKey(fullPath)) {
                        titleMap[fullPath] = title
                        titleMap[cleanSrc.substringAfterLast("/")] = title
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing Nav XHTML TOC", e)
        }
    }

    private fun parseFallbackZipHtml(zipEntries: Map<String, ByteArray>): ParsedEpub {
        val htmlEntries = zipEntries.filter { (key, _) ->
            val k = key.lowercase()
            k.endsWith(".html") || k.endsWith(".xhtml") || k.endsWith(".htm")
        }.toSortedMap()

        val chapters = mutableListOf<EpubChapter>()
        var index = 0

        for ((key, bytes) in htmlEntries) {
            val rawHtml = String(bytes, Charsets.UTF_8)
            val paragraphs = parseHtmlToParagraphs(rawHtml)
            if (paragraphs.isNotEmpty()) {
                val title = extractChapterTitle(rawHtml, key, index + 1)
                chapters.add(
                    EpubChapter(
                        chapterIndex = index,
                        title = title,
                        paragraphs = paragraphs
                    )
                )
                index++
            }
        }

        val bookTitle = chapters.firstOrNull()?.paragraphs?.firstOrNull()?.take(40) ?: "Imported EPUB"
        return ParsedEpub(
            title = bookTitle,
            author = "Unknown Author",
            chapters = chapters
        )
    }

    private fun parseHtmlToParagraphs(rawHtml: String): List<String> {
        var cleaned = rawHtml.replace(Regex("(?s)<script.*?>.*?</script>"), "")
            .replace(Regex("(?s)<style.*?>.*?</style>"), "")

        cleaned = cleaned.replace(Regex("(?i)</p>|<br\\s*/?>|</div>|</h1>|</h2>|</h3>|</h4>|</h5>|</h6>"), "\n\n")
        cleaned = cleaned.replace(Regex("<[^>]*>"), "")

        cleaned = cleaned
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

        return cleaned.split(Regex("\n{2,}"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 2 }
    }

    private fun extractChapterTitle(rawHtml: String, fileName: String, index: Int): String {
        val hMatch = Regex("(?i)<h[1-3][^>]*>(.*?)</h[1-3]>").find(rawHtml)
        if (hMatch != null) {
            val titleText = hMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
            if (titleText.isNotBlank()) return titleText
        }
        val fileCleanName = fileName.substringAfterLast("/").substringBeforeLast(".")
            .replace("_", " ").replace("-", " ").capitalizeWords()
        return if (fileCleanName.isNotBlank() && !fileCleanName.startsWith("chapter", ignoreCase = true)) {
            "Chapter $index: $fileCleanName"
        } else {
            "Chapter $index"
        }
    }

    private fun normalizePath(path: String): String =
        path.replace("\\", "/").trim().trimStart('/').lowercase()

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

    fun extractCoverBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipEntries = mutableMapOf<String, ByteArray>()
                val zipStream = ZipInputStream(inputStream)
                var entry = zipStream.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        zipEntries[normalizePath(entry.name)] = zipStream.readBytes()
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }

                val opfPath = findOpfPath(zipEntries) ?: zipEntries.keys.firstOrNull { it.endsWith(".opf") }
                val opfBytes = opfPath?.let { zipEntries[normalizePath(it)] }
                val opfDoc = if (opfBytes != null) {
                    try {
                        DocumentBuilderFactory.newInstance().also { it.isNamespaceAware = false }
                            .newDocumentBuilder().parse(ByteArrayInputStream(opfBytes))
                    } catch (e: Exception) { null }
                } else null

                val manifestMap = mutableMapOf<String, String>()
                if (opfDoc != null) {
                    val itemNodes = opfDoc.getElementsByTagName("item")
                    for (i in 0 until itemNodes.length) {
                        val item = itemNodes.item(i) as Element
                        val id = item.getAttribute("id")
                        val href = item.getAttribute("href")
                        if (id.isNotBlank() && href.isNotBlank()) {
                            manifestMap[id] = href
                        }
                    }
                }

                extractCoverBytesFromZip(zipEntries, opfDoc, opfPath, manifestMap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting cover from EPUB Uri: $uri", e)
            null
        }
    }

    private fun extractCoverBytesFromZip(
        zipEntries: Map<String, ByteArray>,
        opfDoc: org.w3c.dom.Document?,
        opfPath: String?,
        manifestMap: Map<String, String> = emptyMap()
    ): ByteArray? {
        val opfDir = if (opfPath != null && opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""

        // Strategy 1: OPF <meta name="cover" content="item_id"/>
        if (opfDoc != null) {
            try {
                val metaNodes = opfDoc.getElementsByTagName("meta")
                for (i in 0 until metaNodes.length) {
                    val meta = metaNodes.item(i) as Element
                    val name = meta.getAttribute("name")
                    val property = meta.getAttribute("property")
                    val content = meta.getAttribute("content")

                    if ((name.equals("cover", ignoreCase = true) || property.equals("cover-image", ignoreCase = true)) && content.isNotBlank()) {
                        val href = manifestMap[content] ?: content
                        val targetPath = normalizePath(opfDir + href)
                        val bytes = zipEntries[targetPath] ?: zipEntries[normalizePath(href)]
                        if (bytes != null && bytes.isNotEmpty()) return bytes
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed metadata cover check", e)
            }

            // Strategy 2: OPF <item properties="cover-image" href="..."/> or id containing "cover"
            try {
                val itemNodes = opfDoc.getElementsByTagName("item")
                for (i in 0 until itemNodes.length) {
                    val item = itemNodes.item(i) as Element
                    val id = item.getAttribute("id")
                    val href = item.getAttribute("href")
                    val mediaType = item.getAttribute("media-type")
                    val properties = item.getAttribute("properties")

                    if (properties.contains("cover-image") || id.contains("cover", ignoreCase = true)) {
                        if (mediaType.startsWith("image/") || isImageExtension(href)) {
                            val targetPath = normalizePath(opfDir + href)
                            val bytes = zipEntries[targetPath] ?: zipEntries[normalizePath(href)]
                            if (bytes != null && bytes.isNotEmpty()) return bytes
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed item cover check", e)
            }
        }

        // Strategy 3: Check HTML/XHTML entries for <img> or <image> tag
        try {
            val htmlEntries = zipEntries.filterKeys { it.endsWith(".html") || it.endsWith(".xhtml") || it.endsWith(".htm") }
            for ((htmlPath, bytes) in htmlEntries) {
                if (htmlPath.contains("cover") || htmlPath.contains("title")) {
                    val rawHtml = String(bytes, Charsets.UTF_8)
                    val imgMatch = Regex("(?i)<img[^>]+src=[\"']([^\"']+)[\"']").find(rawHtml)
                        ?: Regex("(?i)<image[^>]+href=[\"']([^\"']+)[\"']").find(rawHtml)
                        ?: Regex("(?i)<image[^>]+xlink:href=[\"']([^\"']+)[\"']").find(rawHtml)
                    if (imgMatch != null) {
                        val src = imgMatch.groupValues[1]
                        val htmlDir = if (htmlPath.contains("/")) htmlPath.substringBeforeLast("/") + "/" else ""
                        val targetPath = normalizePath(htmlDir + src)
                        val imgBytes = zipEntries[targetPath] ?: zipEntries[normalizePath(src)]
                        if (imgBytes != null && imgBytes.isNotEmpty()) return imgBytes
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed HTML img cover check", e)
        }

        // Strategy 4: Search zip keys for filename containing "cover", "titlepage", "jacket", "poster", "front"
        val coverKey = zipEntries.keys.firstOrNull { key ->
            val k = key.lowercase()
            isImageExtension(k) && (k.contains("cover") || k.contains("titlepage") || k.contains("jacket") || k.contains("poster") || k.contains("front"))
        }
        if (coverKey != null) {
            val bytes = zipEntries[coverKey]
            if (bytes != null && bytes.isNotEmpty()) return bytes
        }

        // Strategy 5: Largest Image Fallback
        val largestImageKey = zipEntries.filterKeys { isImageExtension(it) }
            .maxByOrNull { it.value.size }?.key
        if (largestImageKey != null) {
            val bytes = zipEntries[largestImageKey]
            if (bytes != null && bytes.size > 2000) return bytes
        }

        return null
    }

    private fun isImageExtension(path: String): Boolean {
        val k = path.lowercase()
        return k.endsWith(".jpg") || k.endsWith(".jpeg") || k.endsWith(".png") || k.endsWith(".webp") || k.endsWith(".gif") || k.endsWith(".svg")
    }
}
