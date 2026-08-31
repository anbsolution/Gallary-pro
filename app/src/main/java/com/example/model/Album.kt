package com.example.model

import android.net.Uri

data class Album(
    val id: String,
    val name: String,
    val coverUri: Uri?,
    val totalCount: Int,
    val photoCount: Int,
    val videoCount: Int,
    val totalSizeBytes: Long
) {
    val formattedSize: String
        get() {
            if (totalSizeBytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var s = totalSizeBytes.toDouble()
            var idx = 0
            while (s >= 1024.0 && idx < units.size - 1) {
                s /= 1024.0
                idx++
            }
            return String.format(java.util.Locale.US, "%.1f %s", s, units[idx])
        }
}
