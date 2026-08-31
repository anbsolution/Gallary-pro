package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.MediaItem
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.ExifInfoSheet
import com.example.ui.components.MediaViewerScreen
import com.example.ui.components.SelectionActionBar
import com.example.ui.viewmodel.GalleryViewModel

enum class GalleryTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    ALBUMS("Albums", Icons.Filled.Folder, Icons.Outlined.Folder),
    FAVORITES("Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    STORAGE("Storage", Icons.Filled.Storage, Icons.Outlined.Storage),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

enum class StorageSubView {
    OVERVIEW,
    DUPLICATES,
    RECYCLE_BIN
}

@Composable
fun MainGalleryScaffold(
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(GalleryTab.HOME) }
    var storageSubView by remember { mutableStateOf(StorageSubView.OVERVIEW) }

    // States from ViewModel
    val mediaList by viewModel.displayedMediaList.collectAsState()
    val rawMedia by viewModel.rawMediaList.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val favorites by viewModel.favoritesList.collectAsState()
    val trashedItems by viewModel.trashedItemsFlow.collectAsState()
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val isScanningDuplicates by viewModel.isScanningDuplicates.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTypeFilter by viewModel.mediaTypeFilter.collectAsState()
    val selectedDateFilter by viewModel.activeDateFilter.collectAsState()
    val selectedSizeFilter by viewModel.activeSizeFilter.collectAsState()
    val selectedSortOption by viewModel.sortOption.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()

    val isViewerOpen by viewModel.isViewerOpen.collectAsState()
    val viewerItems by viewModel.viewerItems.collectAsState()
    val viewerIndex by viewModel.viewerCurrentIndex.collectAsState()
    val isExifSheetOpen by viewModel.isExifSheetOpen.collectAsState()
    val activeExif by viewModel.activeExif.collectAsState()

    val showDeleteDialog by viewModel.showDeleteConfirmDialog.collectAsState()
    val pendingDeleteItems by viewModel.pendingDeleteItems.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val currentThemeMode by viewModel.themeMode.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val confirmBeforeDelete by viewModel.confirmBeforeDelete.collectAsState()

    // Handle snackbar messages
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    // Intercept back button for selection mode or subviews
    if (isSelectionMode) {
        BackHandler {
            viewModel.clearSelection()
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                SelectionActionBar(
                    selectedCount = selectedUris.size,
                    onClearSelection = { viewModel.clearSelection() },
                    onSelectAll = {
                        val currentList = when (currentTab) {
                            GalleryTab.HOME -> mediaList
                            GalleryTab.ALBUMS -> selectedAlbum?.let { album -> rawMedia.filter { it.bucketId == album.id } } ?: rawMedia
                            GalleryTab.FAVORITES -> favorites
                            else -> mediaList
                        }
                        viewModel.selectAll(currentList)
                    },
                    onShare = {
                        val selectedItems = rawMedia.filter { selectedUris.contains(it.uri) }
                        if (selectedItems.isNotEmpty()) {
                            val shareIntent = viewModel.mediaStoreRepo.createShareIntent(
                                selectedItems.map { it.uri },
                                selectedItems.all { it.isVideo }
                            )
                            context.startActivity(Intent.createChooser(shareIntent, "Share Selected"))
                        }
                    },
                    onFavorite = { viewModel.favoriteSelected() },
                    onDelete = {
                        val selectedItems = rawMedia.filter { selectedUris.contains(it.uri) }
                        viewModel.requestDeleteItems(selectedItems)
                    }
                )
            }
        },
        bottomBar = {
            if (!isViewerOpen && !isSelectionMode) {
                Column {
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        thickness = 1.dp
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        GalleryTab.values().forEach { tab ->
                            val isSelected = tab == currentTab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentTab != tab) {
                                        currentTab = tab
                                        if (tab == GalleryTab.STORAGE) {
                                            storageSubView = StorageSubView.OVERVIEW
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = { 
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ) 
                                },
                                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Tab Content
            when (currentTab) {
                GalleryTab.HOME -> {
                    HomeScreen(
                        mediaList = mediaList,
                        isLoading = isLoading,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        selectedTypeFilter = selectedTypeFilter,
                        onTypeFilterSelected = { viewModel.setMediaTypeFilter(it) },
                        selectedDateFilter = selectedDateFilter,
                        onDateFilterSelected = { viewModel.setDateFilter(it) },
                        selectedSizeFilter = selectedSizeFilter,
                        onSizeFilterSelected = { viewModel.setSizeFilter(it) },
                        selectedSortOption = selectedSortOption,
                        onSortOptionSelected = { viewModel.setSortOption(it) },
                        gridColumns = gridColumns,
                        isSelectionMode = isSelectionMode,
                        selectedUris = selectedUris,
                        onItemClick = { item ->
                            if (isSelectionMode) {
                                viewModel.toggleSelection(item.uri)
                            } else {
                                viewModel.openViewer(item, mediaList)
                            }
                        },
                        onItemLongClick = { item ->
                            if (!isSelectionMode) {
                                viewModel.enterSelectionMode(item.uri)
                            } else {
                                viewModel.toggleSelection(item.uri)
                            }
                        },
                        onRefresh = { viewModel.loadMedia() }
                    )
                }

                GalleryTab.ALBUMS -> {
                    val albumMedia = if (selectedAlbum != null) {
                        rawMedia.filter { it.bucketId == selectedAlbum?.id }
                    } else emptyList()

                    AlbumsScreen(
                        albums = albums,
                        selectedAlbum = selectedAlbum,
                        albumMedia = albumMedia,
                        gridColumns = gridColumns,
                        isSelectionMode = isSelectionMode,
                        selectedUris = selectedUris,
                        onSelectAlbum = { viewModel.selectAlbum(it) },
                        onMediaItemClick = { item ->
                            if (isSelectionMode) {
                                viewModel.toggleSelection(item.uri)
                            } else {
                                viewModel.openViewer(item, albumMedia)
                            }
                        },
                        onMediaItemLongClick = { item ->
                            if (!isSelectionMode) {
                                viewModel.enterSelectionMode(item.uri)
                            } else {
                                viewModel.toggleSelection(item.uri)
                            }
                        },
                        onCreateFolderSaf = { parentUri, folderName ->
                            viewModel.createSafFolder(parentUri, folderName)
                        }
                    )
                }

                GalleryTab.FAVORITES -> {
                    FavoritesScreen(
                        favorites = favorites,
                        gridColumns = gridColumns,
                        isSelectionMode = isSelectionMode,
                        selectedUris = selectedUris,
                        onMediaItemClick = { item ->
                            if (isSelectionMode) {
                                viewModel.toggleSelection(item.uri)
                            } else {
                                viewModel.openViewer(item, favorites)
                            }
                        },
                        onMediaItemLongClick = { item ->
                            if (!isSelectionMode) {
                                viewModel.enterSelectionMode(item.uri)
                            } else {
                                viewModel.toggleSelection(item.uri)
                            }
                        }
                    )
                }

                GalleryTab.STORAGE -> {
                    when (storageSubView) {
                        StorageSubView.OVERVIEW -> {
                            StorageScreen(
                                stats = storageStats,
                                trashedCount = trashedItems.size,
                                onNavigateDuplicates = {
                                    storageSubView = StorageSubView.DUPLICATES
                                    viewModel.rescanDuplicates()
                                },
                                onNavigateTrash = { storageSubView = StorageSubView.RECYCLE_BIN },
                                onMediaItemClick = { item ->
                                    viewModel.openViewer(item, storageStats.largestFiles)
                                },
                                onDeleteItem = { item ->
                                    viewModel.requestDeleteItems(listOf(item))
                                }
                            )
                        }

                        StorageSubView.DUPLICATES -> {
                            DuplicatesScreen(
                                duplicateGroups = duplicateGroups,
                                isScanning = isScanningDuplicates,
                                onRescan = { viewModel.rescanDuplicates() },
                                onDeleteItems = { items -> viewModel.requestDeleteItems(items) },
                                onItemClick = { item ->
                                    val all = duplicateGroups.flatMap { it.items }
                                    viewModel.openViewer(item, all)
                                }
                            )
                        }

                        StorageSubView.RECYCLE_BIN -> {
                            RecycleBinScreen(
                                trashedItems = trashedItems,
                                onBack = { storageSubView = StorageSubView.OVERVIEW },
                                onRestore = { viewModel.restoreFromTrash(it) },
                                onEmptyTrash = { viewModel.clearTrash() }
                            )
                        }
                    }
                }

                GalleryTab.SETTINGS -> {
                    SettingsScreen(
                        currentThemeMode = currentThemeMode,
                        onThemeModeChange = { viewModel.setThemeMode(it) },
                        gridColumns = gridColumns,
                        onGridColumnsChange = { viewModel.setGridColumns(it) },
                        sortOption = selectedSortOption,
                        onSortOptionChange = { viewModel.setSortOption(it) },
                        defaultMediaType = selectedTypeFilter,
                        onDefaultMediaTypeChange = { viewModel.setMediaTypeFilter(it) },
                        confirmBeforeDelete = confirmBeforeDelete,
                        onConfirmBeforeDeleteChange = { viewModel.setConfirmBeforeDelete(it) },
                        onRescanMediaStore = { viewModel.loadMedia() },
                        onExportBackup = { uri -> viewModel.exportBackup(uri) },
                        onImportBackup = { uri -> viewModel.importBackup(uri) }
                    )
                }
            }

            // Fullscreen Viewer Overlay
            if (isViewerOpen && viewerItems.isNotEmpty()) {
                MediaViewerScreen(
                    items = viewerItems,
                    initialIndex = viewerIndex,
                    onPageChanged = { viewModel.onViewerPageChanged(it) },
                    onClose = { viewModel.closeViewer() },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onDelete = { item -> viewModel.requestDeleteItems(listOf(item)) },
                    onOpenInfo = { viewModel.openExifSheet() },
                    onRename = { item, newName -> viewModel.renameMedia(item, newName) },
                    onCopySaf = { item, uri -> viewModel.copyMediaToSaf(item, uri) },
                    onMoveSaf = { item, uri -> viewModel.moveMediaToSaf(item, uri) }
                )
            }

            // EXIF Info Modal Bottom Sheet
            if (isExifSheetOpen && viewerItems.isNotEmpty() && viewerIndex in viewerItems.indices) {
                ExifInfoSheet(
                    item = viewerItems[viewerIndex],
                    exif = activeExif,
                    onDismiss = { viewModel.closeExifSheet() },
                    onStripGps = { item -> viewModel.stripGpsPrivacy(item) }
                )
            }

            // Safe Deletion Confirmation Dialog
            if (showDeleteDialog) {
                DeleteConfirmDialog(
                    itemsToDelete = pendingDeleteItems,
                    onConfirm = { viewModel.onConfirmDeleteDialog() },
                    onDismiss = { viewModel.onDismissDeleteDialog() }
                )
            }
        }
    }
}
