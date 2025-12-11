package com.github.alexisvisco.demospringkotlin.service

import com.github.alexisvisco.demospringkotlin.model.*
import com.github.alexisvisco.demospringkotlin.repository.AttachmentRepository
import com.github.alexisvisco.demospringkotlin.repository.ImageVariantSetRepository
import com.github.alexisvisco.demospringkotlin.temporal.imagevariant.ImageVariantWorkflow
import com.github.alexisvisco.demospringkotlin.temporal.imagevariant.ImageVariantWorkflowImpl
import com.github.alexisvisco.demospringkotlin.temporal.imagevariant.ImageVariantWorkflowParams
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField

@Service
class ImageVariantService(
    private val attachmentRepository: AttachmentRepository,
    private val imageVariantSetRepository: ImageVariantSetRepository,
    private val storageService: StorageService,
    private val workflowClient: WorkflowClient
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ImageVariantService::class.java)

        val DEFAULT_VALIDATION_RULES = StorageService.FileValidationRules(
            maxSize = 10 * 1024 * 1024L, // 10MB
            allowedTypes = setOf("image/jpeg", "image/png", "image/webp")
        )
    }

    /**
     * Creates an ImageVariantSet from an uploaded file and triggers workflow to process variants
     * based on field annotations
     */
    @Transactional
    fun createImageVariantSet(
        file: MultipartFile,
        field: KProperty1<*, *>,
        kindType: String? = null,
        kindId: String? = null,
        validationRules: StorageService.FileValidationRules = DEFAULT_VALIDATION_RULES
    ): ImageVariantSet {
        // Upload original image synchronously
        val originalAttachment = storageService.createAttachmentFromFile(file, validationRules)
        storageService.setContent(originalAttachment, "pictures/originals", file.inputStream)
        val savedOriginalAttachment = attachmentRepository.save(originalAttachment)

        // Create ImageVariantSet
        val imageVariantSet = ImageVariantSet(
            originalAttachment = savedOriginalAttachment,
            kind = kindType,
            kindId = kindId
        )
        val savedImageVariantSet = imageVariantSetRepository.save(imageVariantSet)

        MDC.put("uploaded_original_attachment_id", savedOriginalAttachment.id!!)
        MDC.put("created_image_variant_set_id", savedImageVariantSet.id!!)

        // Extract variant configurations from field annotation
        val variantConfigs = extractVariantConfigs(field)

        if (variantConfigs.isEmpty()) {
            throw IllegalStateException("No image variants configured for field: ${field.name}")
        }

        // Start workflow asynchronously to process variants
        val workflowOptions = WorkflowOptions.newBuilder()
            .setTaskQueue("default")
            .setWorkflowId(ImageVariantWorkflowImpl.buildWorkflowId(field.name, savedImageVariantSet.id!!))
            .build()

        MDC.put("created_workflow_id", workflowOptions.workflowId)

        val workflow = workflowClient.newWorkflowStub(ImageVariantWorkflow::class.java, workflowOptions)
        WorkflowClient.start(
            workflow::processImageVariants,
            ImageVariantWorkflowParams(
                imageVariantSetId = savedImageVariantSet.id!!,
                fieldName = field.name,
                variants = variantConfigs
            )
        )

        return savedImageVariantSet
    }

    /**
     * Extracts variant configurations from field annotations
     */
    private fun extractVariantConfigs(field: KProperty1<*, *>): List<ImageVariantConfig> {
        logger.info("Extracting variant configs for field: ${field.name}")

        // Access the Java field to get annotations (Kotlin property annotations aren't on the property reference)
        val javaField = field.javaField
            ?: throw IllegalArgumentException("Cannot access Java field for property: ${field.name}")

        // Debug: List all annotations on the Java field
        logger.info("Java field annotations: ${javaField.annotations.map { it.annotationClass.simpleName }}")

        val variantConfigs = mutableListOf<ImageVariantConfig>()

        // Check for preset annotation on Java field
        javaField.getAnnotation(ImageVariantPreset::class.java)?.let { presetAnnotation ->
            logger.info("Found ImageVariantPreset annotation with preset: ${presetAnnotation.preset}")
            variantConfigs.addAll(ImageVariantPresets.getVariants(presetAnnotation.preset))
        }

        // Check for individual variant specs on Java field (supports @Repeatable)
        javaField.getAnnotationsByType(ImageVariantSpec::class.java).forEach { spec ->
            logger.info("Found ImageVariantSpec annotation: ${spec.name}")
            variantConfigs.add(
                ImageVariantConfig(
                    name = spec.name,
                    width = spec.width,
                    height = spec.height,
                    ratio = spec.ratio,
                    rotation = spec.rotation,
                    flipHorizontal = spec.flipHorizontal,
                    flipVertical = spec.flipVertical,
                    quality = spec.quality,
                    format = spec.format
                )
            )
        }

        logger.info("Extracted ${variantConfigs.size} variant configurations")
        return variantConfigs
    }
}
