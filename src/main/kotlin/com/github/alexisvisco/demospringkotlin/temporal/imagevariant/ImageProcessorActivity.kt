package com.github.alexisvisco.demospringkotlin.temporal.imagevariant

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.alexisvisco.demospringkotlin.model.*
import com.github.alexisvisco.demospringkotlin.repository.AttachmentRepository
import com.github.alexisvisco.demospringkotlin.repository.ImageVariantSetRepository
import com.github.alexisvisco.demospringkotlin.service.StorageService
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.webp.WebpWriter
import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod
import io.temporal.spring.boot.ActivityImpl
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@ActivityInterface
interface ImageProcessorActivity {
    @ActivityMethod
    fun processImageVariant(params: ProcessImageVariantParams): String
}

data class ProcessImageVariantParams(
    @JsonProperty("imageVariantSetId")
    val imageVariantSetId: String,
    @JsonProperty("variantConfig")
    val variantConfig: ImageVariantConfig
)

@Component
@ActivityImpl(taskQueues = ["default"])
class ImageProcessorActivityImpl(
    private val imageVariantSetRepository: ImageVariantSetRepository,
    private val attachmentRepository: AttachmentRepository,
    private val storageService: StorageService
) : ImageProcessorActivity {

    companion object {
        private val logger = LoggerFactory.getLogger(ImageProcessorActivityImpl::class.java)
    }

    @Transactional
    override fun processImageVariant(params: ProcessImageVariantParams): String {
        val startTime = System.currentTimeMillis()

        try {
            val imageVariantSet = imageVariantSetRepository.findById(params.imageVariantSetId)
                .orElseThrow { IllegalArgumentException("ImageVariantSet not found: ${params.imageVariantSetId}") }

            val originalAttachment = imageVariantSet.originalAttachment

            // Download and load original image
            val image = loadImage(originalAttachment)

            // Apply all transformations
            val transformedImage = applyTransformations(image, params.variantConfig)

            // Export the transformed image with the specified format and quality
            val (imageBytes, format, extension, contentType) = exportImage(
                transformedImage,
                params.variantConfig,
                originalAttachment.contentType
            )

            // Generate descriptive filename for the variant
            val variantFilename = generateVariantFilename(
                originalAttachment.id!!,
                params.variantConfig,
                extension
            )

            // Create and save the variant attachment
            val variantAttachment = createVariantAttachment(
                variantFilename,
                contentType,
                imageBytes
            )

            // Create and persist the ImageVariant record
            val imageVariant = createImageVariant(
                variantAttachment,
                imageVariantSet,
                params.variantConfig,
                transformedImage,
                format
            )

            imageVariantSet.variants.add(imageVariant)
            imageVariantSetRepository.save(imageVariantSet)

            val processingTime = System.currentTimeMillis() - startTime
            logger.atInfo()
                .addKeyValue("image_variant_name", params.variantConfig.name)
                .addKeyValue("original_attachment_id", originalAttachment.id)
                .addKeyValue("variant_attachment_id", variantAttachment.id)
                .addKeyValue("processing_time_ms", processingTime)
                .addKeyValue("width", transformedImage.width)
                .addKeyValue("height", transformedImage.height)
                .addKeyValue("format", format)
                .log("processed image variant")

            return variantAttachment.id!!

        } catch (e: Exception) {
            logger.error("Failed to process image variant: ${e.message}", e)
            throw RuntimeException("Failed to process image variant: ${e.message}", e)
        }
    }

    /**
     * Loads image from storage using streaming to minimize memory usage
     */
    private fun loadImage(attachment: Attachment): ImmutableImage {
        return try {
            val imageStream = storageService.getContent(attachment)
            imageStream.use { stream ->
                ImmutableImage.loader().fromStream(stream)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to load image: ${e.message}", e)
        }
    }

    /**
     * Applies all transformations to the image in the correct order:
     * 1. Rotation
     * 2. Flips (horizontal/vertical)
     * 3. Resizing based on dimensions and aspect ratio
     */
    private fun applyTransformations(
        image: ImmutableImage,
        config: ImageVariantConfig
    ): ImmutableImage {
        var result = image

        // Apply rotation first
        if (config.rotation != 0) {
            result = applyRotation(result, config.rotation)
        }

        // Apply flips
        if (config.flipHorizontal) {
            result = result.flipX()
        }
        if (config.flipVertical) {
            result = result.flipY()
        }

        // Calculate and apply target dimensions
        val (targetWidth, targetHeight) = calculateDimensions(result, config)

        if (targetWidth != result.width || targetHeight != result.height) {
            result = resizeImage(result, targetWidth, targetHeight)
        }

        return result
    }

    /**
     * Applies rotation transformation
     */
    private fun applyRotation(image: ImmutableImage, rotation: Int): ImmutableImage {
        return when (rotation) {
            90 -> image.rotateRight()
            180 -> image.rotateRight().rotateRight()
            270 -> image.rotateLeft()
            else -> {
                logger.warn("Unsupported rotation angle: $rotation, skipping rotation")
                image
            }
        }
    }

    /**
     * Calculates target dimensions based on configuration
     * Mirrors the Go implementation's dimension calculation logic
     */
    private fun calculateDimensions(
        image: ImmutableImage,
        config: ImageVariantConfig
    ): Pair<Int, Int> {
        val originalWidth = image.width
        val originalHeight = image.height
        val originalRatio = originalWidth.toDouble() / originalHeight.toDouble()

        // If both width and height are specified, use them directly
        if (config.width > 0 && config.height > 0) {
            return Pair(config.width, config.height)
        }

        // If ratio is specified, use it to calculate missing dimension
        if (config.ratio > 0.0) {
            if (config.width > 0) {
                return Pair(config.width, (config.width / config.ratio).toInt())
            }
            if (config.height > 0) {
                return Pair((config.height * config.ratio).toInt(), config.height)
            }
            // If only ratio is specified, maintain original area
            val area = originalWidth * originalHeight
            val newWidth = (area * config.ratio / (1 + config.ratio)).toInt()
            val newHeight = (newWidth / config.ratio).toInt()
            return Pair(newWidth, newHeight)
        }

        // If only width is specified, maintain aspect ratio
        if (config.width > 0) {
            return Pair(config.width, (config.width / originalRatio).toInt())
        }

        // If only height is specified, maintain aspect ratio
        if (config.height > 0) {
            return Pair((config.height * originalRatio).toInt(), config.height)
        }

        // No dimensions specified, return original
        return Pair(originalWidth, originalHeight)
    }

    /**
     * Resizes image to target dimensions using high-quality scaling
     */
    private fun resizeImage(
        image: ImmutableImage,
        targetWidth: Int,
        targetHeight: Int
    ): ImmutableImage {
        return try {
            // Use fit() for consistent behavior with aspect ratio preservation
            image.fit(targetWidth, targetHeight)
        } catch (e: Exception) {
            throw RuntimeException("Failed to resize image to ${targetWidth}x${targetHeight}: ${e.message}", e)
        }
    }

    /**
     * Exports the image to the specified format with quality settings
     * Returns: (imageBytes, format, extension, contentType)
     */
    private fun exportImage(
        image: ImmutableImage,
        config: ImageVariantConfig,
        originalContentType: String
    ): ImageExportResult {
        // Determine format (use config format, or fallback to original)
        return when (config.format) {
            ImageFormat.JPEG -> {
                val quality = calculateQualityForJpeg(config.quality)
                val writer = JpegWriter().withCompression(quality)
                ImageExportResult(
                    bytes = image.bytes(writer),
                    format = "JPEG",
                    extension = "jpg",
                    contentType = "image/jpeg"
                )
            }

            ImageFormat.PNG -> {
                val compression = calculateCompressionForPng(config.quality)
                val writer = if (compression >= 9) {
                    PngWriter.MaxCompression
                } else {
                    PngWriter.NoCompression
                }
                ImageExportResult(
                    bytes = image.bytes(writer),
                    format = "PNG",
                    extension = "png",
                    contentType = "image/png"
                )
            }

            ImageFormat.WEBP -> {
                val quality = calculateQualityForWebp(config.quality)
                val writer = WebpWriter.DEFAULT.withQ(quality)
                ImageExportResult(
                    bytes = image.bytes(writer),
                    format = "WEBP",
                    extension = "webp",
                    contentType = "image/webp"
                )
            }

            ImageFormat.DEFAULT -> {
                // Keep original format
                when (originalContentType) {
                    "image/jpeg" -> {
                        val quality = if (config.quality > 0.0) (config.quality * 100).toInt() else 90
                        ImageExportResult(
                            bytes = image.bytes(JpegWriter().withCompression(quality)),
                            format = "JPEG",
                            extension = "jpg",
                            contentType = "image/jpeg"
                        )
                    }

                    "image/png" -> ImageExportResult(
                        bytes = image.bytes(PngWriter.MaxCompression),
                        format = "PNG",
                        extension = "png",
                        contentType = "image/png"
                    )

                    "image/webp" -> {
                        val quality = if (config.quality > 0.0) (config.quality * 100).toInt() else 85
                        ImageExportResult(
                            bytes = image.bytes(WebpWriter.DEFAULT.withQ(quality)),
                            format = "WEBP",
                            extension = "webp",
                            contentType = "image/webp"
                        )
                    }

                    else -> {
                        // Default to JPEG for unknown types
                        ImageExportResult(
                            bytes = image.bytes(JpegWriter().withCompression(90)),
                            format = "JPEG",
                            extension = "jpg",
                            contentType = "image/jpeg"
                        )
                    }
                }
            }
        }
    }

    /**
     * Calculates JPEG quality from 0-1 range to 1-100 range
     */
    private fun calculateQualityForJpeg(quality: Double): Int {
        return if (quality > 0.0) {
            maxOf(1, minOf(100, (quality * 100).toInt()))
        } else {
            90 // default quality
        }
    }

    /**
     * Calculates PNG compression from 0-1 quality range to 0-9 compression range (inverted)
     */
    private fun calculateCompressionForPng(quality: Double): Int {
        return if (quality > 0.0) {
            minOf(maxOf(((1.0 - quality) * 9).toInt(), 0), 9)
        } else {
            9 // default maximum compression
        }
    }

    /**
     * Calculates WebP quality from 0-1 range to 0-100 range
     */
    private fun calculateQualityForWebp(quality: Double): Int {
        return if (quality > 0.0) {
            maxOf(0, minOf(100, (quality * 100).toInt()))
        } else {
            85 // default quality
        }
    }

    /**
     * Generates a descriptive filename for the variant based on transformations
     * Example: att_abc123_variant_w800_h600_q90.jpg
     */
    private fun generateVariantFilename(
        originalAttachmentId: String,
        config: ImageVariantConfig,
        extension: String
    ): String {
        val parts = mutableListOf(originalAttachmentId, "variant")

        if (config.width > 0) {
            parts.add("w${config.width}")
        }
        if (config.height > 0) {
            parts.add("h${config.height}")
        }
        if (config.ratio > 0.0) {
            parts.add("r%.2f".format(config.ratio))
        }
        if (config.rotation != 0) {
            parts.add("rot${config.rotation}")
        }
        if (config.flipHorizontal) {
            parts.add("fliph")
        }
        if (config.flipVertical) {
            parts.add("flipv")
        }
        if (config.quality > 0.0) {
            parts.add("q${(config.quality * 100).toInt()}")
        }

        return "${parts.joinToString("_")}.$extension"
    }

    /**
     * Creates and saves a variant attachment
     */
    private fun createVariantAttachment(
        filename: String,
        contentType: String,
        imageBytes: ByteArray
    ): Attachment {
        val attachment = Attachment(
            filename = filename,
            contentType = contentType,
            byteSize = imageBytes.size.toLong(),
            key = "",
            checksum = ""
        )

        storageService.setContent(attachment, "image-variants", imageBytes.inputStream())
        return attachmentRepository.save(attachment)
    }

    /**
     * Creates an ImageVariant record with metadata
     */
    private fun createImageVariant(
        attachment: Attachment,
        imageVariantSet: ImageVariantSet,
        config: ImageVariantConfig,
        transformedImage: ImmutableImage,
        format: String
    ): ImageVariant {
        return ImageVariant(
            attachment = attachment,
            imageVariantSet = imageVariantSet,
            name = config.name,
            metadata = mapOf(
                "width" to transformedImage.width,
                "height" to transformedImage.height,
                "format" to format,
                "quality" to config.quality
            )
        )
    }
}

/**
 * Result of image export operation
 */
private data class ImageExportResult(
    val bytes: ByteArray,
    val format: String,
    val extension: String,
    val contentType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageExportResult

        if (!bytes.contentEquals(other.bytes)) return false
        if (format != other.format) return false
        if (extension != other.extension) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + extension.hashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}
