package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.data.parser.PdfHelper
import com.example.ui.theme.ReaderThemeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfPagePagingSource(
    private val context: Context,
    private val fileUri: Uri,
    private val isAsset: Boolean,
    private val assetName: String?,
    private val pageCount: Int,
    private val targetWidthPx: Int = 1080
) : PagingSource<Int, PdfPageItem>() {

    override fun getRefreshKey(state: PagingState<Int, PdfPageItem>): Int? {
        return state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PdfPageItem> {
        val pageIndex = params.key ?: 0
        if (pageIndex < 0 || pageIndex >= pageCount) {
            return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
        }

        val bitmap = withContext(Dispatchers.IO) {
            PdfHelper.renderPageBitmap(
                context = context,
                fileUri = fileUri,
                isAsset = isAsset,
                assetName = assetName,
                pageIndex = pageIndex,
                targetWidth = targetWidthPx
            )
        }

        val item = PdfPageItem(
            pageIndex = pageIndex,
            pageCount = pageCount,
            bitmap = bitmap
        )

        val prevKey = if (pageIndex > 0) pageIndex - 1 else null
        val nextKey = if (pageIndex < pageCount - 1) pageIndex + 1 else null

        return LoadResult.Page(
            data = listOf(item),
            prevKey = prevKey,
            nextKey = nextKey
        )
    }
}

data class PdfPageItem(
    val pageIndex: Int,
    val pageCount: Int,
    val bitmap: Bitmap?
)

@Composable
fun ScrollablePdfDocumentView(
    fileUri: Uri,
    isAsset: Boolean,
    assetName: String?,
    pageCount: Int,
    initialPageIndex: Int,
    themeColors: ReaderThemeColors,
    onPageSelected: (Int) -> Unit,
    onToggleImmersive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0)))

    // Dynamic bitmap memory cache for fast smooth scrolling
    val pageBitmaps = remember(fileUri, assetName, pageCount) {
        mutableStateMapOf<Int, Bitmap?>()
    }

    // Monitor visible page index to update progress and pre-fetch adjacent pages
    val currentVisiblePage by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(currentVisiblePage) {
        onPageSelected(currentVisiblePage)

        // Pre-fetch adjacent pages in background coroutines (current - 1 to current + 3)
        withContext(Dispatchers.IO) {
            val pagesToPreload = listOf(
                currentVisiblePage,
                currentVisiblePage + 1,
                currentVisiblePage - 1,
                currentVisiblePage + 2,
                currentVisiblePage + 3
            ).filter { it in 0 until pageCount }

            for (p in pagesToPreload) {
                if (!pageBitmaps.containsKey(p)) {
                    val rendered = PdfHelper.renderPageBitmap(
                        context = context,
                        fileUri = fileUri,
                        isAsset = isAsset,
                        assetName = assetName,
                        pageIndex = p,
                        targetWidth = 960
                    )
                    if (rendered != null) {
                        pageBitmaps[p] = rendered
                    }
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.backgroundColor)
            .testTag("pdf_scrollable_document_view"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(
            count = pageCount,
            key = { index -> "pdf_page_$index" }
        ) { pageIndex ->
            val bitmap = pageBitmaps[pageIndex]

            LaunchedEffect(pageIndex) {
                if (bitmap == null) {
                    val rendered = withContext(Dispatchers.IO) {
                        PdfHelper.renderPageBitmap(
                            context = context,
                            fileUri = fileUri,
                            isAsset = isAsset,
                            assetName = assetName,
                            pageIndex = pageIndex,
                            targetWidth = 960
                        )
                    }
                    if (rendered != null) {
                        pageBitmaps[pageIndex] = rendered
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColors.surfaceColor)
                    .clickable { onToggleImmersive() }
                    .testTag("pdf_page_card_$pageIndex"),
                color = themeColors.surfaceColor,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header label inside page card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(themeColors.surfaceColor)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DOCUMENT PAGE ${pageIndex + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.accentColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${pageIndex + 1} / $pageCount",
                            fontSize = 12.sp,
                            color = themeColors.textColor.copy(alpha = 0.6f)
                        )
                    }

                    Divider(color = themeColors.textColor.copy(alpha = 0.1f))

                    // PDF Page Image Content
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "PDF Document Page ${pageIndex + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            contentScale = ContentScale.FillWidth
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .background(themeColors.surfaceColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = themeColors.accentColor,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Rendering Page ${pageIndex + 1}...",
                                    fontSize = 12.sp,
                                    color = themeColors.textColor.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Bottom subtle footer inside page card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(themeColors.surfaceColor)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "- Page ${pageIndex + 1} -",
                            fontSize = 11.sp,
                            color = themeColors.textColor.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}
