package com.github.alexisvisco.demospringkotlin.model

import com.fasterxml.jackson.annotation.JsonProperty

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ImageVariantSpec(
    val name: String,
    val width: Int = 0,           // 0 means unspecified
    val height: Int = 0,          // 0 means unspecified
    val ratio: Double = 0.0,      // 0 means unspecified
    val rotation: Int = 0,        // 0 means no rotation
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val quality: Double = 0.0,    // 0 means use default quality (1.0)
    val format: ImageFormat = ImageFormat.DEFAULT
)

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ImageVariantPreset(
    val preset: ImagePreset
)

enum class ImagePreset {
    PROFILE_PICTURE,  // thumbnail, small, medium sizes
    PRODUCT_IMAGE,    // small, medium, large, xlarge with webp
    BANNER,           // wide aspect ratios
    GALLERY,          // various sizes optimized for gallery view
    CUSTOM            // use individual @ImageVariantSpec annotations
}

enum class ImageFormat {
    DEFAULT,  // Keep original format
    JPEG,
    PNG,
    WEBP
}

// Preset definitions
object ImageVariantPresets {
    fun getVariants(preset: ImagePreset): List<ImageVariantConfig> {
        return when (preset) {
            ImagePreset.PROFILE_PICTURE -> listOf(
                ImageVariantConfig("thumbnail", width = 150, format = ImageFormat.WEBP),
                ImageVariantConfig("small", width = 300, format = ImageFormat.WEBP),
                ImageVariantConfig("medium", width = 600, format = ImageFormat.WEBP)
            )

            ImagePreset.PRODUCT_IMAGE -> listOf(
                ImageVariantConfig("small", width = 400, format = ImageFormat.WEBP),
                ImageVariantConfig("medium", width = 800, format = ImageFormat.WEBP),
                ImageVariantConfig("large", width = 1200, format = ImageFormat.WEBP),
                ImageVariantConfig("xlarge", width = 1600, format = ImageFormat.WEBP)
            )

            ImagePreset.BANNER -> listOf(
                ImageVariantConfig("small", width = 800, format = ImageFormat.WEBP),
                ImageVariantConfig("medium", width = 1200, format = ImageFormat.WEBP),
                ImageVariantConfig("large", width = 1920, format = ImageFormat.WEBP)
            )

            ImagePreset.GALLERY -> listOf(
                ImageVariantConfig("thumbnail", width = 200, format = ImageFormat.WEBP),
                ImageVariantConfig("preview", width = 600, format = ImageFormat.WEBP),
                ImageVariantConfig("full", width = 1920, format = ImageFormat.WEBP)
            )

            ImagePreset.CUSTOM -> emptyList()
        }
    }
}

data class ImageVariantConfig(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("width")
    val width: Int = 0,
    @JsonProperty("height")
    val height: Int = 0,
    @JsonProperty("ratio")
    val ratio: Double = 0.0,
    @JsonProperty("rotation")
    val rotation: Int = 0,
    @JsonProperty("flipHorizontal")
    val flipHorizontal: Boolean = false,
    @JsonProperty("flipVertical")
    val flipVertical: Boolean = false,
    @JsonProperty("quality")
    val quality: Double = 0.0,
    @JsonProperty("format")
    val format: ImageFormat = ImageFormat.DEFAULT
)
