package com.example.model

enum class MediaTypeFilter(val label: String) {
    ALL("All Media"),
    PHOTOS("Photos"),
    VIDEOS("Videos")
}

enum class DateFilter(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year")
}

enum class SizeFilter(val label: String) {
    ALL("Any Size"),
    UNDER_5MB("< 5 MB"),
    FROM_5_TO_25MB("5 - 25 MB"),
    FROM_25_TO_100MB("25 - 100 MB"),
    OVER_100MB("> 100 MB")
}

enum class SortOption(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    SIZE_DESC("Largest First"),
    SIZE_ASC("Smallest First"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)")
}
