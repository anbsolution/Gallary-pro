package com.example.model

data class DuplicateGroup(
    val groupKey: String,
    val items: List<MediaItem>,
    val fileSize: Long,
    val potentialSavingsBytes: Long
) {
    val formattedSavings: String
        get() {
            if (potentialSavingsBytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var s = potentialSavingsBytes.toDouble()
            var idx = 0
            while (s >= 1024.0 && idx < units.size - 1) {
                s /= 1024.0
                idx++
            }
            return String.format(java.util.Locale.US, "%.1f %s", s, units[idx])
        }
}
