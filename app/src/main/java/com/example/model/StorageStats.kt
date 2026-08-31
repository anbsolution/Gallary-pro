package com.example.model

data class StorageStats(
    val totalMediaCount: Int = 0,
    val imageCount: Int = 0,
    val videoCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val imagesTotalSizeBytes: Long = 0L,
    val videosTotalSizeBytes: Long = 0L,
    val duplicateGroupCount: Int = 0,
    val duplicateSavingsBytes: Long = 0L,
    val largestFiles: List<MediaItem> = emptyList()
) {
    val formattedTotalSize: String get() = formatBytes(totalSizeBytes)
    val formattedImagesSize: String get() = formatBytes(imagesTotalSizeBytes)
    val formattedVideosSize: String get() = formatBytes(videosTotalSizeBytes)
    val formattedDuplicateSavings: String get() = formatBytes(duplicateSavingsBytes)

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var s = bytes.toDouble()
        var idx = 0
        while (s >= 1024.0 && idx < units.size - 1) {
            s /= 1024.0
            idx++
        }
        return String.format(java.util.Locale.US, "%.1f %s", s, units[idx])
    }
}
