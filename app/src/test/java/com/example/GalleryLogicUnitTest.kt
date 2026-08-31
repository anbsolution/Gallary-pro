package com.example

import android.net.Uri
import com.example.model.DateFilter
import com.example.model.DuplicateGroup
import com.example.model.MediaItem
import com.example.model.MediaTypeFilter
import com.example.model.SizeFilter
import com.example.model.SortOption
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GalleryLogicUnitTest {

    private fun createDummyItem(
        id: Long,
        displayName: String,
        size: Long,
        dateTaken: Long,
        isVideo: Boolean = false,
        bucketDisplayName: String = "Camera"
    ): MediaItem {
        return MediaItem(
            id = id,
            uri = Uri.parse("content://media/external/images/media/$id"),
            path = "/storage/emulated/0/$bucketDisplayName/$displayName",
            displayName = displayName,
            title = displayName.substringBeforeLast("."),
            mimeType = if (isVideo) "video/mp4" else "image/jpeg",
            size = size,
            width = 1920,
            height = 1080,
            dateAdded = dateTaken / 1000,
            dateTaken = dateTaken,
            dateModified = dateTaken / 1000,
            durationMs = if (isVideo) 60000L else 0L,
            isVideo = isVideo,
            bucketId = "1001",
            bucketDisplayName = bucketDisplayName,
            isFavorite = false,
            isTrashed = false
        )
    }

    @Test
    fun testMediaSorting_byName_ascAndDesc() {
        val item1 = createDummyItem(1, "Alpha.jpg", 1000, 1000)
        val item2 = createDummyItem(2, "Beta.jpg", 2000, 2000)
        val item3 = createDummyItem(3, "Gamma.jpg", 3000, 3000)

        val list = listOf(item2, item3, item1)

        val sortedAsc = list.sortedBy { it.displayName.lowercase() }
        assertEquals("Alpha.jpg", sortedAsc[0].displayName)
        assertEquals("Beta.jpg", sortedAsc[1].displayName)
        assertEquals("Gamma.jpg", sortedAsc[2].displayName)

        val sortedDesc = list.sortedByDescending { it.displayName.lowercase() }
        assertEquals("Gamma.jpg", sortedDesc[0].displayName)
        assertEquals("Beta.jpg", sortedDesc[1].displayName)
        assertEquals("Alpha.jpg", sortedDesc[2].displayName)
    }

    @Test
    fun testMediaSorting_byDate() {
        val oldItem = createDummyItem(1, "Old.jpg", 1000, 100000L)
        val newItem = createDummyItem(2, "New.jpg", 1000, 500000L)

        val list = listOf(oldItem, newItem)
        val sortedNewestFirst = list.sortedByDescending { it.dateTaken }

        assertEquals("New.jpg", sortedNewestFirst.first().displayName)
        assertEquals("Old.jpg", sortedNewestFirst.last().displayName)
    }

    @Test
    fun testMediaSorting_bySize() {
        val small = createDummyItem(1, "Small.jpg", 500, 1000)
        val large = createDummyItem(2, "Large.jpg", 5000000, 1000)

        val list = listOf(small, large)
        val sortedLargestFirst = list.sortedByDescending { it.size }

        assertEquals("Large.jpg", sortedLargestFirst.first().displayName)
        assertEquals("Small.jpg", sortedLargestFirst.last().displayName)
    }

    @Test
    fun testMediaTypeFilter_imagesAndVideos() {
        val photo = createDummyItem(1, "Photo.jpg", 1000, 1000, isVideo = false)
        val video = createDummyItem(2, "Video.mp4", 5000, 1000, isVideo = true)

        val list = listOf(photo, video)

        val photosOnly = list.filter { !it.isVideo }
        assertEquals(1, photosOnly.size)
        assertEquals("Photo.jpg", photosOnly[0].displayName)

        val videosOnly = list.filter { it.isVideo }
        assertEquals(1, videosOnly.size)
        assertEquals("Video.mp4", videosOnly[0].displayName)
    }

    @Test
    fun testSearchFilter_matchesDisplayName() {
        val photo1 = createDummyItem(1, "IMG_2026_Family.jpg", 1000, 1000)
        val photo2 = createDummyItem(2, "IMG_2026_Nature.jpg", 1000, 1000)
        val photo3 = createDummyItem(3, "Screenshot_123.png", 1000, 1000)

        val list = listOf(photo1, photo2, photo3)
        val query = "family"

        val results = list.filter { it.displayName.contains(query, ignoreCase = true) }
        assertEquals(1, results.size)
        assertEquals("IMG_2026_Family.jpg", results[0].displayName)
    }

    @Test
    fun testDuplicateGrouping_savingsCalculation() {
        val item1 = createDummyItem(1, "IMG_1.jpg", 2000000, 1000)
        val item2 = createDummyItem(2, "IMG_1_copy.jpg", 2000000, 1000)
        val item3 = createDummyItem(3, "IMG_1_copy2.jpg", 2000000, 1000)

        val items = listOf(item1, item2, item3)
        val group = DuplicateGroup(
            groupKey = "hash_2000000_1920_1080",
            items = items,
            fileSize = 2000000L,
            potentialSavingsBytes = 2000000L * (items.size - 1)
        )

        // 3 items of 2,000,000 bytes => keep 1, 2 copies can be deleted => potential savings = 4,000,000 bytes
        assertEquals(4000000L, group.potentialSavingsBytes)
        assertEquals("3.8 MB", group.formattedSavings)
    }

    @Test
    fun testBackupJsonFormat_generationAndParsing() {
        val favorites = listOf("content://media/external/images/media/1", "content://media/external/images/media/2")
        val json = JSONObject().apply {
            put("version", 1)
            put("timestamp", 123456789L)
            put("favorites", JSONArray(favorites))
            put("gridColumns", 3)
            put("confirmBeforeDelete", true)
            put("sortOption", "DATE_DESC")
            put("themeMode", "DARK")
            put("defaultMediaType", "ALL")
        }

        val jsonStr = json.toString()
        val parsed = JSONObject(jsonStr)

        assertEquals(1, parsed.getInt("version"))
        assertEquals(3, parsed.getInt("gridColumns"))
        assertTrue(parsed.getBoolean("confirmBeforeDelete"))
        assertEquals("DATE_DESC", parsed.getString("sortOption"))

        val parsedFavs = parsed.getJSONArray("favorites")
        assertEquals(2, parsedFavs.length())
        assertEquals("content://media/external/images/media/1", parsedFavs.getString(0))
        assertEquals("content://media/external/images/media/2", parsedFavs.getString(1))
    }
}
