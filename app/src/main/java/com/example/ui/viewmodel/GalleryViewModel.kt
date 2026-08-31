package com.example.ui.viewmodel

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.FavoriteEntity
import com.example.data.db.GalleryDatabase
import com.example.data.db.TrashEntity
import com.example.data.repository.GalleryPreferences
import com.example.data.repository.MediaStoreRepository
import com.example.model.Album
import com.example.model.AppThemeMode
import com.example.model.DateFilter
import com.example.model.DuplicateGroup
import com.example.model.ExifMetadata
import com.example.model.MediaItem
import com.example.model.MediaTypeFilter
import com.example.model.SizeFilter
import com.example.model.SortOption
import com.example.model.StorageStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    val mediaStoreRepo = MediaStoreRepository(application)
    val preferences = GalleryPreferences(application)
    private val database = GalleryDatabase.getDatabase(application)
    private val favoriteDao = database.favoriteDao()
    private val trashDao = database.trashDao()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isScanningDuplicates = MutableStateFlow(false)
    val isScanningDuplicates: StateFlow<Boolean> = _isScanningDuplicates.asStateFlow()

    private val _rawMediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val rawMediaList: StateFlow<List<MediaItem>> = _rawMediaList.asStateFlow()

    val favoriteUrisFlow = favoriteDao.getAllFavoriteUrisFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val trashedItemsFlow = trashDao.getAllTrashedFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    // Filters & Navigation States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum.asStateFlow()

    val mediaTypeFilter = preferences.defaultMediaType
    val sortOption = preferences.sortOption
    val themeMode = preferences.themeMode
    val gridColumns = preferences.gridColumns
    val confirmBeforeDelete = preferences.confirmBeforeDelete

    private val _activeDateFilter = MutableStateFlow(DateFilter.ALL_TIME)
    val activeDateFilter: StateFlow<DateFilter> = _activeDateFilter.asStateFlow()

    private val _activeSizeFilter = MutableStateFlow(SizeFilter.ALL)
    val activeSizeFilter: StateFlow<SizeFilter> = _activeSizeFilter.asStateFlow()

    // Selection Mode
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedUris = MutableStateFlow<Set<Uri>>(emptySet())
    val selectedUris: StateFlow<Set<Uri>> = _selectedUris.asStateFlow()

    // Full-screen Viewer State
    private val _isViewerOpen = MutableStateFlow(false)
    val isViewerOpen: StateFlow<Boolean> = _isViewerOpen.asStateFlow()

    private val _viewerItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val viewerItems: StateFlow<List<MediaItem>> = _viewerItems.asStateFlow()

    private val _viewerCurrentIndex = MutableStateFlow(0)
    val viewerCurrentIndex: StateFlow<Int> = _viewerCurrentIndex.asStateFlow()

    // EXIF Metadata Sheet
    private val _activeExif = MutableStateFlow<ExifMetadata?>(null)
    val activeExif: StateFlow<ExifMetadata?> = _activeExif.asStateFlow()

    private val _isExifSheetOpen = MutableStateFlow(false)
    val isExifSheetOpen: StateFlow<Boolean> = _isExifSheetOpen.asStateFlow()

    // Pending Deletion Flow (Scoped storage IntentSender or Direct)
    private val _pendingDeleteIntentSender = MutableStateFlow<IntentSender?>(null)
    val pendingDeleteIntentSender: StateFlow<IntentSender?> = _pendingDeleteIntentSender.asStateFlow()

    private val _pendingDeleteItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val pendingDeleteItems: StateFlow<List<MediaItem>> = _pendingDeleteItems.asStateFlow()

    private val _showDeleteConfirmDialog = MutableStateFlow(false)
    val showDeleteConfirmDialog: StateFlow<Boolean> = _showDeleteConfirmDialog.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Merged & Filtered Media Flow
    @Suppress("UNCHECKED_CAST")
    val displayedMediaList: StateFlow<List<MediaItem>> = combine(
        _rawMediaList,
        favoriteUrisFlow,
        _searchQuery,
        _selectedAlbum,
        mediaTypeFilter,
        _activeDateFilter,
        _activeSizeFilter,
        sortOption
    ) { params ->
        val raw = params[0] as List<MediaItem>
        val favs = (params[1] as List<String>).toSet()
        val query = (params[2] as String).trim().lowercase()
        val album = params[3] as Album?
        val typeFilter = params[4] as MediaTypeFilter
        val dateF = params[5] as DateFilter
        val sizeF = params[6] as SizeFilter
        val sort = params[7] as SortOption

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // Decorate with favorite status
        var filtered = raw.map { item ->
            item.copy(isFavorite = favs.contains(item.uri.toString()))
        }

        // Album Filter
        if (album != null) {
            filtered = filtered.filter { it.bucketId == album.id }
        }

        // Media Type Filter
        filtered = when (typeFilter) {
            MediaTypeFilter.ALL -> filtered
            MediaTypeFilter.PHOTOS -> filtered.filter { !it.isVideo }
            MediaTypeFilter.VIDEOS -> filtered.filter { it.isVideo }
        }

        // Search Filter
        if (query.isNotEmpty()) {
            filtered = filtered.filter { item ->
                item.displayName.lowercase().contains(query) ||
                        item.title.lowercase().contains(query) ||
                        item.bucketDisplayName.lowercase().contains(query) ||
                        item.mimeType.lowercase().contains(query)
            }
        }

        // Date Filter
        if (dateF != DateFilter.ALL_TIME) {
            cal.timeInMillis = now
            val startOfDay = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            filtered = when (dateF) {
                DateFilter.TODAY -> filtered.filter { it.dateTaken >= startOfDay }
                DateFilter.THIS_WEEK -> {
                    val startOfWeek = cal.apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
                    filtered.filter { it.dateTaken >= startOfWeek }
                }
                DateFilter.THIS_MONTH -> {
                    val startOfMonth = cal.apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
                    filtered.filter { it.dateTaken >= startOfMonth }
                }
                DateFilter.THIS_YEAR -> {
                    val startOfYear = cal.apply { add(Calendar.DAY_OF_YEAR, -365) }.timeInMillis
                    filtered.filter { it.dateTaken >= startOfYear }
                }
                else -> filtered
            }
        }

        // Size Filter
        filtered = when (sizeF) {
            SizeFilter.ALL -> filtered
            SizeFilter.UNDER_5MB -> filtered.filter { it.size < 5 * 1024 * 1024L }
            SizeFilter.FROM_5_TO_25MB -> filtered.filter { it.size in (5 * 1024 * 1024L)..(25 * 1024 * 1024L) }
            SizeFilter.FROM_25_TO_100MB -> filtered.filter { it.size in (25 * 1024 * 1024L)..(100 * 1024 * 1024L) }
            SizeFilter.OVER_100MB -> filtered.filter { it.size > 100 * 1024 * 1024L }
        }

        // Sorting
        when (sort) {
            SortOption.DATE_DESC -> filtered.sortedByDescending { if (it.dateTaken > 0) it.dateTaken else it.dateModified }
            SortOption.DATE_ASC -> filtered.sortedBy { if (it.dateTaken > 0) it.dateTaken else it.dateModified }
            SortOption.SIZE_DESC -> filtered.sortedByDescending { it.size }
            SortOption.SIZE_ASC -> filtered.sortedBy { it.size }
            SortOption.NAME_ASC -> filtered.sortedBy { it.displayName.lowercase() }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.displayName.lowercase() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Favorites List
    val favoritesList: StateFlow<List<MediaItem>> = combine(
        _rawMediaList,
        favoriteUrisFlow
    ) { mediaList, favUris ->
        val favSet = favUris.toSet()
        mediaList.filter { favSet.contains(it.uri.toString()) }
            .map { it.copy(isFavorite = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onPermissionGranted() {
        _hasPermission.value = true
        loadMedia()
    }

    fun onPermissionDenied() {
        _hasPermission.value = false
        _rawMediaList.value = emptyList()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val media = mediaStoreRepo.queryAllMedia()
                _rawMediaList.value = media
                _albums.value = mediaStoreRepo.getAlbums(media)
                val duplicates = mediaStoreRepo.findDuplicates(media)
                _duplicateGroups.value = duplicates
                _storageStats.value = mediaStoreRepo.computeStorageStats(media, duplicates)
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarMessage.value = "Failed to scan media: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rescanDuplicates() {
        viewModelScope.launch {
            _isScanningDuplicates.value = true
            try {
                val duplicates = mediaStoreRepo.findDuplicates(_rawMediaList.value)
                _duplicateGroups.value = duplicates
                _storageStats.value = mediaStoreRepo.computeStorageStats(_rawMediaList.value, duplicates)
                _snackbarMessage.value = "Found ${duplicates.size} duplicate groups"
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanningDuplicates.value = false
            }
        }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            val uriStr = item.uri.toString()
            val isFav = favoriteDao.isFavorite(uriStr) > 0
            if (isFav) {
                favoriteDao.deleteFavorite(uriStr)
                _snackbarMessage.value = "Removed from Favorites"
            } else {
                favoriteDao.insertFavorite(FavoriteEntity(mediaUriString = uriStr))
                _snackbarMessage.value = "Added to Favorites"
            }
        }
    }

    fun toggleSelection(uri: Uri) {
        val current = _selectedUris.value.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        _selectedUris.value = current
        if (current.isEmpty()) {
            _isSelectionMode.value = false
        } else {
            _isSelectionMode.value = true
        }
    }

    fun enterSelectionMode(firstUri: Uri) {
        _isSelectionMode.value = true
        _selectedUris.value = setOf(firstUri)
    }

    fun selectAll(items: List<MediaItem>) {
        _selectedUris.value = items.map { it.uri }.toSet()
        _isSelectionMode.value = true
    }

    fun clearSelection() {
        _selectedUris.value = emptySet()
        _isSelectionMode.value = false
    }

    fun favoriteSelected() {
        viewModelScope.launch {
            val uris = _selectedUris.value
            uris.forEach { uri ->
                favoriteDao.insertFavorite(FavoriteEntity(mediaUriString = uri.toString()))
            }
            _snackbarMessage.value = "Added ${uris.size} items to Favorites"
            clearSelection()
        }
    }

    // Delete Requests Handling
    fun requestDeleteItems(items: List<MediaItem>) {
        if (items.isEmpty()) return
        _pendingDeleteItems.value = items
        if (confirmBeforeDelete.value) {
            _showDeleteConfirmDialog.value = true
        } else {
            executePendingDelete()
        }
    }

    fun onConfirmDeleteDialog() {
        _showDeleteConfirmDialog.value = false
        executePendingDelete()
    }

    fun onDismissDeleteDialog() {
        _showDeleteConfirmDialog.value = false
        _pendingDeleteItems.value = emptyList()
    }

    private fun executePendingDelete() {
        val items = _pendingDeleteItems.value
        if (items.isEmpty()) return

        viewModelScope.launch {
            val uris = items.map { it.uri }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intentSender = mediaStoreRepo.createDeleteIntentSender(uris)
                if (intentSender != null) {
                    _pendingDeleteIntentSender.value = intentSender
                    // Waiting for Activity Result launcher
                    return@launch
                }
            }

            // Fallback direct delete
            val deletedCount = mediaStoreRepo.deleteDirectly(uris)
            items.forEach { item ->
                trashDao.insertTrashed(
                    TrashEntity(
                        mediaUriString = item.uri.toString(),
                        displayName = item.displayName,
                        size = item.size,
                        isVideo = item.isVideo,
                        originalPath = item.path
                    )
                )
            }
            _snackbarMessage.value = "Deleted $deletedCount item(s)"
            clearSelection()
            _pendingDeleteItems.value = emptyList()
            loadMedia()
        }
    }

    fun onScopedStorageDeleteResult(success: Boolean) {
        val items = _pendingDeleteItems.value
        _pendingDeleteIntentSender.value = null
        if (success && items.isNotEmpty()) {
            viewModelScope.launch {
                items.forEach { item ->
                    trashDao.insertTrashed(
                        TrashEntity(
                            mediaUriString = item.uri.toString(),
                            displayName = item.displayName,
                            size = item.size,
                            isVideo = item.isVideo,
                            originalPath = item.path
                        )
                    )
                }
                _snackbarMessage.value = "Deleted ${items.size} item(s)"
                clearSelection()
                _pendingDeleteItems.value = emptyList()
                loadMedia()
            }
        } else {
            _snackbarMessage.value = "Deletion cancelled"
            _pendingDeleteItems.value = emptyList()
        }
    }

    fun restoreFromTrash(entity: TrashEntity) {
        viewModelScope.launch {
            trashDao.deleteTrashed(entity.mediaUriString)
            _snackbarMessage.value = "Restored ${entity.displayName}"
        }
    }

    fun clearTrash() {
        viewModelScope.launch {
            trashDao.clearAll()
            _snackbarMessage.value = "Trash emptied"
        }
    }

    // Fullscreen Viewer
    fun openViewer(item: MediaItem, fromList: List<MediaItem>) {
        _viewerItems.value = fromList
        val index = fromList.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
        _viewerCurrentIndex.value = index
        _isViewerOpen.value = true
        fetchExifForItem(item)
    }

    fun onViewerPageChanged(newIndex: Int) {
        val list = _viewerItems.value
        if (newIndex in list.indices) {
            _viewerCurrentIndex.value = newIndex
            fetchExifForItem(list[newIndex])
        }
    }

    fun closeViewer() {
        _isViewerOpen.value = false
        _activeExif.value = null
        _isExifSheetOpen.value = false
    }

    fun openExifSheet() {
        _isExifSheetOpen.value = true
    }

    fun closeExifSheet() {
        _isExifSheetOpen.value = false
    }

    private fun fetchExifForItem(item: MediaItem) {
        viewModelScope.launch {
            val exif = mediaStoreRepo.getExifMetadata(item)
            _activeExif.value = exif
        }
    }

    // Filter & Search Controls
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectAlbum(album: Album?) {
        _selectedAlbum.value = album
    }

    fun setMediaTypeFilter(filter: MediaTypeFilter) {
        preferences.setDefaultMediaType(filter)
    }

    fun setDateFilter(filter: DateFilter) {
        _activeDateFilter.value = filter
    }

    fun setSizeFilter(filter: SizeFilter) {
        _activeSizeFilter.value = filter
    }

    fun setSortOption(option: SortOption) {
        preferences.setSortOption(option)
    }

    fun setGridColumns(columns: Int) {
        preferences.setGridColumns(columns)
    }

    fun setThemeMode(mode: AppThemeMode) {
        preferences.setThemeMode(mode)
    }

    fun setConfirmBeforeDelete(confirm: Boolean) {
        preferences.setConfirmBeforeDelete(confirm)
    }

    fun renameMedia(item: MediaItem, newName: String) {
        viewModelScope.launch {
            val success = mediaStoreRepo.renameMedia(item, newName)
            if (success) {
                _snackbarMessage.value = "Renamed to $newName"
                loadMedia()
            } else {
                _snackbarMessage.value = "Failed to rename (permission restricted)"
            }
        }
    }

    fun copyMediaToSaf(item: MediaItem, targetTreeUri: Uri) {
        viewModelScope.launch {
            val resultUri = mediaStoreRepo.copyMediaToSafFolder(
                sourceUri = item.uri,
                targetTreeUri = targetTreeUri,
                displayName = item.displayName,
                mimeType = item.mimeType
            )
            if (resultUri != null) {
                _snackbarMessage.value = "Copied ${item.displayName} successfully"
            } else {
                _snackbarMessage.value = "Copy failed"
            }
        }
    }

    fun moveMediaToSaf(item: MediaItem, targetTreeUri: Uri) {
        viewModelScope.launch {
            val resultUri = mediaStoreRepo.copyMediaToSafFolder(
                sourceUri = item.uri,
                targetTreeUri = targetTreeUri,
                displayName = item.displayName,
                mimeType = item.mimeType
            )
            if (resultUri != null) {
                _snackbarMessage.value = "Moved ${item.displayName}"
                requestDeleteItems(listOf(item))
            } else {
                _snackbarMessage.value = "Move failed"
            }
        }
    }

    fun createSafFolder(parentTreeUri: Uri, folderName: String) {
        viewModelScope.launch {
            val created = mediaStoreRepo.createSafFolder(parentTreeUri, folderName)
            if (created != null) {
                _snackbarMessage.value = "Created folder: $folderName"
            } else {
                _snackbarMessage.value = "Failed to create folder"
            }
        }
    }

    fun stripGpsPrivacy(item: MediaItem) {
        viewModelScope.launch {
            val newUri = mediaStoreRepo.stripGpsAndSaveClean(item)
            if (newUri != null) {
                _snackbarMessage.value = "Clean photo saved without GPS metadata"
                loadMedia()
            } else {
                _snackbarMessage.value = "Could not strip GPS metadata"
            }
        }
    }

    fun exportBackup(outputUri: Uri) {
        viewModelScope.launch {
            try {
                val favs = favoriteDao.getAllFavoriteUris()
                val jsonObject = org.json.JSONObject().apply {
                    put("version", 1)
                    put("timestamp", System.currentTimeMillis())
                    put("favorites", org.json.JSONArray(favs))
                    put("gridColumns", gridColumns.value)
                    put("confirmBeforeDelete", confirmBeforeDelete.value)
                    put("sortOption", sortOption.value.name)
                    put("themeMode", themeMode.value.name)
                    put("defaultMediaType", mediaTypeFilter.value.name)
                }
                getApplication<Application>().contentResolver.openOutputStream(outputUri)?.use { stream ->
                    stream.write(jsonObject.toString(2).toByteArray(Charsets.UTF_8))
                }
                _snackbarMessage.value = "Backup exported successfully!"
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarMessage.value = "Export failed: ${e.localizedMessage}"
            }
        }
    }

    fun importBackup(inputUri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = getApplication<Application>().contentResolver.openInputStream(inputUri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: throw IllegalStateException("Empty backup file")

                val jsonObject = org.json.JSONObject(jsonString)
                val favArray = jsonObject.optJSONArray("favorites")
                if (favArray != null) {
                    for (i in 0 until favArray.length()) {
                        val uriStr = favArray.getString(i)
                        favoriteDao.insertFavorite(FavoriteEntity(mediaUriString = uriStr))
                    }
                }
                if (jsonObject.has("gridColumns")) {
                    preferences.setGridColumns(jsonObject.getInt("gridColumns"))
                }
                if (jsonObject.has("confirmBeforeDelete")) {
                    preferences.setConfirmBeforeDelete(jsonObject.getBoolean("confirmBeforeDelete"))
                }
                if (jsonObject.has("sortOption")) {
                    try {
                        preferences.setSortOption(SortOption.valueOf(jsonObject.getString("sortOption")))
                    } catch (_: Exception) {}
                }
                if (jsonObject.has("themeMode")) {
                    try {
                        preferences.setThemeMode(AppThemeMode.valueOf(jsonObject.getString("themeMode")))
                    } catch (_: Exception) {}
                }
                if (jsonObject.has("defaultMediaType")) {
                    try {
                        preferences.setDefaultMediaType(MediaTypeFilter.valueOf(jsonObject.getString("defaultMediaType")))
                    } catch (_: Exception) {}
                }
                _snackbarMessage.value = "Backup restored successfully!"
                loadMedia()
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarMessage.value = "Restore failed: ${e.localizedMessage}"
            }
        }
    }

    fun clearPendingDeleteIntentSender() {
        _pendingDeleteIntentSender.value = null
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}

class GalleryViewModelFactory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            return GalleryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

