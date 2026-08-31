package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewerScreen(
    items: List<MediaItem>,
    initialIndex: Int,
    onPageChanged: (Int) -> Unit,
    onClose: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit,
    onOpenInfo: (MediaItem) -> Unit,
    onRename: ((MediaItem, String) -> Unit)? = null,
    onCopySaf: ((MediaItem, Uri) -> Unit)? = null,
    onMoveSaf: ((MediaItem, Uri) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        onClose()
        return
    }

    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, items.size - 1),
        pageCount = { items.size }
    )

    var showControls by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var pendingSafAction by remember { mutableStateOf<String?>(null) }

    val currentItem = items.getOrElse(pagerState.currentPage) { items.first() }

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        treeUri?.let { uri ->
            if (pendingSafAction == "copy") {
                onCopySaf?.invoke(currentItem, uri)
            } else if (pendingSafAction == "move") {
                onMoveSaf?.invoke(currentItem, uri)
            }
        }
        pendingSafAction = null
    }

    BackHandler {
        onClose()
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChanged(page)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val item = items[page]
            if (item.isVideo) {
                VideoPlayerItem(
                    item = item,
                    isActive = page == pagerState.currentPage,
                    onToggleControls = { showControls = !showControls }
                )
            } else {
                ZoomableImageItem(
                    item = item,
                    onToggleControls = { showControls = !showControls }
                )
            }
        }

        // Top Overlay Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("viewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = currentItem.displayName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val dateFmt = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                        val dateStr = dateFmt.format(Date(if (currentItem.dateTaken > 0) currentItem.dateTaken else currentItem.dateModified))
                        Text(
                            text = "${pagerState.currentPage + 1} / ${items.size} • $dateStr",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = { onOpenInfo(currentItem) },
                        modifier = Modifier.testTag("viewer_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Metadata Info",
                            tint = Color.White
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("viewer_more_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename File") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    renameText = currentItem.displayName
                                    showRenameDialog = true
                                },
                                modifier = Modifier.testTag("menu_rename")
                            )
                            DropdownMenuItem(
                                text = { Text("Copy to Folder (SAF)") },
                                leadingIcon = { Icon(Icons.Default.FileCopy, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    pendingSafAction = "copy"
                                    safFolderLauncher.launch(null)
                                },
                                modifier = Modifier.testTag("menu_copy_saf")
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Folder (SAF)") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    pendingSafAction = "move"
                                    safFolderLauncher.launch(null)
                                },
                                modifier = Modifier.testTag("menu_move_saf")
                            )
                        }
                    }
                }
            }
        }

        // Rename Dialog
        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Media File") },
                text = {
                    Column {
                        Text(
                            text = "Enter a new filename including extension:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("rename_input_field")
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (renameText.isNotBlank() && renameText != currentItem.displayName) {
                                onRename?.invoke(currentItem, renameText.trim())
                            }
                            showRenameDialog = false
                        },
                        modifier = Modifier.testTag("confirm_rename_button")
                    ) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Bottom Overlay Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = if (currentItem.isVideo) "video/*" else "image/*"
                                putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share ${currentItem.displayName}"))
                        },
                        modifier = Modifier.testTag("viewer_share_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text("Share", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    // Favorite
                    IconButton(
                        onClick = { onToggleFavorite(currentItem) },
                        modifier = Modifier.testTag("viewer_favorite_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (currentItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (currentItem.isFavorite) Color(0xFFF43F5E) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text("Favorite", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    // Open With
                    IconButton(
                        onClick = {
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(currentItem.uri, currentItem.mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(Intent.createChooser(viewIntent, "Open with"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.testTag("viewer_open_with_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open with",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text("Open", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    // Delete
                    IconButton(
                        onClick = { onDelete(currentItem) },
                        modifier = Modifier.testTag("viewer_delete_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                            Text("Delete", color = Color(0xFFEF4444), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomableImageItem(
    item: MediaItem,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2.5f
                        offset = Offset.Zero
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        val maxOffsetX = (size.width * (scale - 1)) / 2f
                        val maxOffsetY = (size.height * (scale - 1)) / 2f
                        val newOffset = offset + pan
                        offset = Offset(
                            newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                            newOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

@Composable
fun VideoPlayerItem(
    item: MediaItem,
    isActive: Boolean,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(item.durationMs.toInt()) }
    var isDraggingSlider by remember { mutableStateOf(false) }

    // Coroutine to poll current playback position
    LaunchedEffect(isPlaying, isActive) {
        while (isActive && isPlaying) {
            videoViewRef?.let { vv ->
                if (!isDraggingSlider && vv.isPlaying) {
                    currentPosition = vv.currentPosition
                    if (vv.duration > 0) duration = vv.duration
                }
            }
            delay(250)
        }
    }

    LaunchedEffect(isActive) {
        if (!isActive) {
            videoViewRef?.pause()
            isPlaying = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggleControls() })
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    setVideoURI(item.uri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = false
                        duration = mp.duration
                    }
                    setOnCompletionListener {
                        isPlaying = false
                        isCompleted = true
                    }
                    videoViewRef = this
                }
            },
            update = {
                videoViewRef = it
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center Play / Pause Floating Overlay Button
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.Center)
        ) {
            IconButton(
                onClick = {
                    videoViewRef?.let { vv ->
                        if (isPlaying) {
                            vv.pause()
                            isPlaying = false
                        } else {
                            if (isCompleted) {
                                vv.seekTo(0)
                                isCompleted = false
                            }
                            vv.start()
                            isPlaying = true
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = when {
                        isCompleted -> Icons.Default.Replay
                        isPlaying -> Icons.Default.Pause
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Bottom Progress & Timestamp Controls
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(currentPosition.toLong()),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                    onValueChange = { fraction ->
                        isDraggingSlider = true
                        currentPosition = (fraction * duration).toInt()
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        videoViewRef?.seekTo(currentPosition)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Text(
                    text = formatTime(duration.toLong()),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
