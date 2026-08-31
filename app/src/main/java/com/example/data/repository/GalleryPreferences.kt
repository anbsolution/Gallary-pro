package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppThemeMode
import com.example.model.MediaTypeFilter
import com.example.model.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GalleryPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gallery_pro_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _gridColumns = MutableStateFlow(loadGridColumns())
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    private val _sortOption = MutableStateFlow(loadSortOption())
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _defaultMediaType = MutableStateFlow(loadDefaultMediaType())
    val defaultMediaType: StateFlow<MediaTypeFilter> = _defaultMediaType.asStateFlow()

    private val _confirmBeforeDelete = MutableStateFlow(loadConfirmBeforeDelete())
    val confirmBeforeDelete: StateFlow<Boolean> = _confirmBeforeDelete.asStateFlow()

    private val _showVideoDurationBadge = MutableStateFlow(loadShowVideoDurationBadge())
    val showVideoDurationBadge: StateFlow<Boolean> = _showVideoDurationBadge.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    private fun loadThemeMode(): AppThemeMode {
        val name = prefs.getString("theme_mode", AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(name ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setGridColumns(cols: Int) {
        val clamped = cols.coerceIn(2, 5)
        prefs.edit().putInt("grid_columns", clamped).apply()
        _gridColumns.value = clamped
    }

    private fun loadGridColumns(): Int {
        return prefs.getInt("grid_columns", 3).coerceIn(2, 5)
    }

    fun setSortOption(option: SortOption) {
        prefs.edit().putString("sort_option", option.name).apply()
        _sortOption.value = option
    }

    private fun loadSortOption(): SortOption {
        val name = prefs.getString("sort_option", SortOption.DATE_DESC.name)
        return try {
            SortOption.valueOf(name ?: SortOption.DATE_DESC.name)
        } catch (e: Exception) {
            SortOption.DATE_DESC
        }
    }

    fun setDefaultMediaType(filter: MediaTypeFilter) {
        prefs.edit().putString("default_media_type", filter.name).apply()
        _defaultMediaType.value = filter
    }

    private fun loadDefaultMediaType(): MediaTypeFilter {
        val name = prefs.getString("default_media_type", MediaTypeFilter.ALL.name)
        return try {
            MediaTypeFilter.valueOf(name ?: MediaTypeFilter.ALL.name)
        } catch (e: Exception) {
            MediaTypeFilter.ALL
        }
    }

    fun setConfirmBeforeDelete(confirm: Boolean) {
        prefs.edit().putBoolean("confirm_before_delete", confirm).apply()
        _confirmBeforeDelete.value = confirm
    }

    private fun loadConfirmBeforeDelete(): Boolean {
        return prefs.getBoolean("confirm_before_delete", true)
    }

    fun setShowVideoDurationBadge(show: Boolean) {
        prefs.edit().putBoolean("show_video_duration_badge", show).apply()
        _showVideoDurationBadge.value = show
    }

    private fun loadShowVideoDurationBadge(): Boolean {
        return prefs.getBoolean("show_video_duration_badge", true)
    }
}
