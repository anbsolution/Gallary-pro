package com.example.data.repository

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.example.model.Album
import com.example.model.DuplicateGroup
import com.example.model.ExifMetadata
import com.example.model.MediaItem
import com.example.model.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

class MediaStoreRepository(private val context: Context) {

    private val contentResolver = context.contentResolver

    suspend fun queryAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.TITLE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATA,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.DATE_TAKEN else MediaStore.MediaColumns.DATE_MODIFIED,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.BUCKET_ID else MediaStore.MediaColumns._ID,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.BUCKET_DISPLAY_NAME else MediaStore.MediaColumns.DISPLAY_NAME,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.DURATION else MediaStore.MediaColumns._ID,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.IS_TRASHED else MediaStore.MediaColumns._ID
        )

        // 1. Query Images
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(
                imageUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val dateModCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val dateTakenCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                } else -1
                val bucketIdCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
                } else -1
                val bucketNameCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                } else -1
                val isTrashedCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(imageUri, id)
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "IMG_$id" else "IMG_$id"
                    val title = if (titleCol >= 0) cursor.getString(titleCol) ?: name else name
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "image/*" else "image/*"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val dateAdded = if (dateAddedCol >= 0) cursor.getLong(dateAddedCol) * 1000L else 0L
                    val dateMod = if (dateModCol >= 0) cursor.getLong(dateModCol) * 1000L else 0L
                    val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else dateMod
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "default" else "default"
                    val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "Pictures" else "Pictures"
                    val isTrashed = if (isTrashedCol >= 0) cursor.getInt(isTrashedCol) == 1 else false

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            path = path,
                            displayName = name,
                            title = title,
                            mimeType = mime,
                            size = size,
                            width = width,
                            height = height,
                            dateAdded = dateAdded,
                            dateTaken = if (dateTaken > 0) dateTaken else dateMod,
                            dateModified = dateMod,
                            durationMs = 0L,
                            isVideo = false,
                            bucketId = bucketId,
                            bucketDisplayName = bucketName,
                            isTrashed = isTrashed
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Query Videos
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        try {
            contentResolver.query(
                videoUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val dateModCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val dateTakenCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                } else -1
                val bucketIdCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
                } else -1
                val bucketNameCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                } else -1
                val durationCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
                } else -1
                val isTrashedCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(videoUri, id)
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "VID_$id" else "VID_$id"
                    val title = if (titleCol >= 0) cursor.getString(titleCol) ?: name else name
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "video/*" else "video/*"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val dateAdded = if (dateAddedCol >= 0) cursor.getLong(dateAddedCol) * 1000L else 0L
                    val dateMod = if (dateModCol >= 0) cursor.getLong(dateModCol) * 1000L else 0L
                    val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else dateMod
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "default_video" else "default_video"
                    val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "Videos" else "Videos"
                    val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                    val isTrashed = if (isTrashedCol >= 0) cursor.getInt(isTrashedCol) == 1 else false

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            path = path,
                            displayName = name,
                            title = title,
                            mimeType = mime,
                            size = size,
                            width = width,
                            height = height,
                            dateAdded = dateAdded,
                            dateTaken = if (dateTaken > 0) dateTaken else dateMod,
                            dateModified = dateMod,
                            durationMs = duration,
                            isVideo = true,
                            bucketId = bucketId,
                            bucketDisplayName = bucketName,
                            isTrashed = isTrashed
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort initially by date taken / modified descending
        mediaList.sortedByDescending { if (it.dateTaken > 0) it.dateTaken else it.dateModified }
    }

    suspend fun getAlbums(mediaList: List<MediaItem>): List<Album> = withContext(Dispatchers.Default) {
        val grouped = mediaList.filter { !it.isTrashed }.groupBy { it.bucketId }
        grouped.map { (bucketId, items) ->
            val first = items.firstOrNull()
            val name = first?.bucketDisplayName?.takeIf { it.isNotBlank() } ?: "Folder"
            val photoCount = items.count { !it.isVideo }
            val videoCount = items.count { it.isVideo }
            val totalSize = items.sumOf { it.size }
            Album(
                id = bucketId,
                name = name,
                coverUri = first?.uri,
                totalCount = items.size,
                photoCount = photoCount,
                videoCount = videoCount,
                totalSizeBytes = totalSize
            )
        }.sortedByDescending { it.totalCount }
    }

    suspend fun computeStorageStats(mediaList: List<MediaItem>, duplicates: List<DuplicateGroup>): StorageStats = withContext(Dispatchers.Default) {
        val nonTrashed = mediaList.filter { !it.isTrashed }
        val images = nonTrashed.filter { !it.isVideo }
        val videos = nonTrashed.filter { it.isVideo }

        val imageBytes = images.sumOf { it.size }
        val videoBytes = videos.sumOf { it.size }
        val totalBytes = imageBytes + videoBytes

        val dupSavings = duplicates.sumOf { it.potentialSavingsBytes }
        val largest = nonTrashed.sortedByDescending { it.size }.take(20)

        StorageStats(
            totalMediaCount = nonTrashed.size,
            imageCount = images.size,
            videoCount = videos.size,
            totalSizeBytes = totalBytes,
            imagesTotalSizeBytes = imageBytes,
            videosTotalSizeBytes = videoBytes,
            duplicateGroupCount = duplicates.size,
            duplicateSavingsBytes = dupSavings,
            largestFiles = largest
        )
    }

    suspend fun findDuplicates(mediaList: List<MediaItem>): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val nonTrashed = mediaList.filter { !it.isTrashed && it.size > 1024L } // filter zero or tiny files

        // Group first by size + dimensions (cheap check)
        val sizeGrouped = nonTrashed.groupBy { "${it.size}_${it.width}_${it.height}_${it.isVideo}" }
            .filter { it.value.size > 1 }

        val duplicateGroups = mutableListOf<DuplicateGroup>()

        for ((_, candidateItems) in sizeGrouped) {
            // Further verify candidates by sampling first 8KB hash to avoid full file I/O
            val hashGroups = candidateItems.groupBy { item ->
                getSampleHeaderHash(item.uri) ?: item.displayName
            }

            for ((hashKey, matchedItems) in hashGroups) {
                if (matchedItems.size > 1) {
                    val singleSize = matchedItems.first().size
                    val potentialSavings = singleSize * (matchedItems.size - 1)
                    duplicateGroups.add(
                        DuplicateGroup(
                            groupKey = hashKey,
                            items = matchedItems.sortedByDescending { it.dateTaken },
                            fileSize = singleSize,
                            potentialSavingsBytes = potentialSavings
                        )
                    )
                }
            }
        }

        duplicateGroups.sortedByDescending { it.potentialSavingsBytes }
    }

    private fun getSampleHeaderHash(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(8192)
                val bytesRead = stream.read(buffer, 0, buffer.size)
                if (bytesRead <= 0) return null
                val digest = MessageDigest.getInstance("MD5")
                digest.update(buffer, 0, bytesRead)
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getExifMetadata(item: MediaItem): ExifMetadata = withContext(Dispatchers.IO) {
        if (item.isVideo) {
            return@withContext ExifMetadata(
                width = item.width,
                height = item.height,
                dateOriginal = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(item.dateTaken))
            )
        }

        try {
            val targetUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    MediaStore.setRequireOriginal(item.uri)
                } catch (e: Exception) {
                    item.uri
                }
            } else {
                item.uri
            }

            contentResolver.openInputStream(targetUri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                val expTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                val focalLen = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                val flash = exif.getAttribute(ExifInterface.TAG_FLASH)
                val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

                val latLongArray = exif.latLong
                val hasGps = latLongArray != null
                val altitude = exif.getAltitude(0.0)

                val apertureStr = fNumber?.toDoubleOrNull()?.let { "f/${String.format(java.util.Locale.US, "%.1f", it)}" } ?: fNumber
                val exposureStr = expTime?.toDoubleOrNull()?.let {
                    if (it < 1.0 && it > 0.0) "1/${(1.0 / it).toInt()}s" else "${it}s"
                } ?: expTime
                val focalStr = focalLen?.toDoubleOrNull()?.let { "${String.format(java.util.Locale.US, "%.1f", it)} mm" } ?: focalLen

                ExifMetadata(
                    make = make?.trim(),
                    model = model?.trim(),
                    aperture = apertureStr,
                    exposureTime = exposureStr,
                    iso = iso?.let { "ISO $it" },
                    focalLength = focalStr,
                    flash = flash,
                    dateOriginal = dateTime,
                    width = if (item.width > 0) item.width else exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0),
                    height = if (item.height > 0) item.height else exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0),
                    orientationDegrees = exif.rotationDegrees,
                    latitude = if (latLongArray != null && latLongArray.size >= 2) latLongArray[0] else null,
                    longitude = if (latLongArray != null && latLongArray.size >= 2) latLongArray[1] else null,
                    altitude = if (hasGps && altitude > 0.0) altitude else null
                )
            } ?: ExifMetadata(width = item.width, height = item.height)
        } catch (e: Exception) {
            e.printStackTrace()
            ExifMetadata(width = item.width, height = item.height)
        }
    }

    /**
     * Rename media item. On Android 10+, updates MediaColumns.DISPLAY_NAME in MediaStore.
     */
    suspend fun renameMedia(item: MediaItem, newDisplayName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val values = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName)
                put(MediaStore.MediaColumns.TITLE, newDisplayName.substringBeforeLast("."))
            }
            val rows = contentResolver.update(item.uri, values, null, null)
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Copy a media file into a user-selected folder via Storage Access Framework (SAF).
     */
    suspend fun copyMediaToSafFolder(sourceUri: Uri, targetTreeUri: Uri, displayName: String, mimeType: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val docUri = if (android.provider.DocumentsContract.isDocumentUri(context, targetTreeUri)) {
                targetTreeUri
            } else {
                val docId = android.provider.DocumentsContract.getTreeDocumentId(targetTreeUri)
                android.provider.DocumentsContract.buildDocumentUriUsingTree(targetTreeUri, docId)
            }

            val createdDocUri = android.provider.DocumentsContract.createDocument(
                contentResolver,
                docUri,
                mimeType,
                displayName
            ) ?: return@withContext null

            contentResolver.openInputStream(sourceUri)?.use { input ->
                contentResolver.openOutputStream(createdDocUri)?.use { output ->
                    input.copyTo(output)
                }
            }
            createdDocUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Create a new folder inside a SAF tree URI.
     */
    suspend fun createSafFolder(parentTreeUri: Uri, folderName: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val docUri = if (android.provider.DocumentsContract.isDocumentUri(context, parentTreeUri)) {
                parentTreeUri
            } else {
                val docId = android.provider.DocumentsContract.getTreeDocumentId(parentTreeUri)
                android.provider.DocumentsContract.buildDocumentUriUsingTree(parentTreeUri, docId)
            }

            android.provider.DocumentsContract.createDocument(
                contentResolver,
                docUri,
                android.provider.DocumentsContract.Document.MIME_TYPE_DIR,
                folderName
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Strip GPS / Location metadata and save a clean copy to MediaStore.
     */
    suspend fun stripGpsAndSaveClean(item: MediaItem): Uri? = withContext(Dispatchers.IO) {
        if (item.isVideo) return@withContext null
        try {
            val cleanName = "Clean_${System.currentTimeMillis()}_${item.displayName}"
            val values = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, cleanName)
                put(MediaStore.Images.Media.MIME_TYPE, item.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GalleryPro_Clean")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val newUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext null

            // Read image and decode/compress without GPS EXIF
            contentResolver.openInputStream(item.uri)?.use { inStream ->
                val bitmap = android.graphics.BitmapFactory.decodeStream(inStream)
                if (bitmap != null) {
                    contentResolver.openOutputStream(newUri)?.use { outStream ->
                        val compressFormat = if (item.mimeType.contains("png", true)) {
                            android.graphics.Bitmap.CompressFormat.PNG
                        } else {
                            android.graphics.Bitmap.CompressFormat.JPEG
                        }
                        bitmap.compress(compressFormat, 95, outStream)
                    }
                } else {
                    // Fallback copy stream directly
                    contentResolver.openOutputStream(newUri)?.use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(newUri, values, null, null)
            }

            newUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Prepares deletion for media items.
     * On Android 11+ (API 30+), returns an IntentSender from MediaStore.createDeleteRequest.
     * On Android 10 and below, deletes directly via contentResolver.
     */
    suspend fun createDeleteIntentSender(uris: List<Uri>): IntentSender? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent: PendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                pendingIntent.intentSender
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    suspend fun deleteDirectly(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (uri in uris) {
            try {
                val deleted = contentResolver.delete(uri, null, null)
                if (deleted > 0) count++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        count
    }

    fun createShareIntent(uris: List<Uri>, isVideo: Boolean): Intent {
        return if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = if (isVideo) "video/*" else "image/*"
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
}
