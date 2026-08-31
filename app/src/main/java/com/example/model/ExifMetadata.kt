package com.example.model

data class ExifMetadata(
    val make: String? = null,
    val model: String? = null,
    val aperture: String? = null,
    val exposureTime: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val flash: String? = null,
    val dateOriginal: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val orientationDegrees: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null
) {
    val hasGps: Boolean get() = latitude != null && longitude != null
    val cameraSummary: String
        get() {
            return when {
                !make.isNullOrBlank() && !model.isNullOrBlank() -> {
                    if (model.startsWith(make, ignoreCase = true)) model else "$make $model"
                }
                !model.isNullOrBlank() -> model
                !make.isNullOrBlank() -> make
                else -> "Unknown Camera"
            }
        }
}
