package com.example.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val path: String,
    val displayName: String,
    val title: String,
    val mimeType: String,
    val size: Long,
    val width: Int,
    val height: Int,
    val dateAdded: Long,
    val dateTaken: Long,
    val dateModified: Long,
    val durationMs: Long = 0L,
    val isVideo: Boolean = false,
    val bucketId: String = "",
    val bucketDisplayName: String = "",
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false
) {
    val formattedDuration: String
        get() {
            if (!isVideo || durationMs <= 0) return ""
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                val remainingMinutes = minutes % 60
                String.format("%d:%02d:%02d", hours, remainingMinutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var s = size.toDouble()
            var idx = 0
            while (s >= 1024.0 && idx < units.size - 1) {
                s /= 1024.0
                idx++
            }
            return String.format(java.util.Locale.US, "%.1f %s", s, units[idx])
        }

    val resolutionString: String
        get() {
            return if (width > 0 && height > 0) {
                val mp = (width.toLong() * height.toLong()) / 1_000_000.0
                if (mp >= 1.0) {
                    String.format(java.util.Locale.US, "%d × %d (%.1f MP)", width, height, mp)
                } else {
                    "${width} × ${height}"
                }
            } else {
                "Unknown"
            }
        }
}
