package com.github.alexisvisco.demospringkotlin.temporal.imagevariant

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.alexisvisco.demospringkotlin.model.ImageVariantConfig
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.spring.boot.WorkflowImpl
import io.temporal.workflow.Workflow
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import org.slf4j.MDC
import java.time.Duration

@WorkflowInterface
interface ImageVariantWorkflow {
    @WorkflowMethod
    fun processImageVariants(params: ImageVariantWorkflowParams): List<String>
}

@WorkflowImpl(taskQueues = ["default"])
class ImageVariantWorkflowImpl : ImageVariantWorkflow {

    companion object {
        fun buildWorkflowId(fieldName: String, kindId: String): String {
            return "image-variant-workflow-$fieldName-$kindId-${System.currentTimeMillis()}"
        }
    }

    private val logger = Workflow.getLogger(ImageVariantWorkflowImpl::class.java)

    private val imageProcessor = Workflow.newActivityStub(
        ImageProcessorActivity::class.java,
        ActivityOptions.newBuilder()
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .build()
            )
            .setStartToCloseTimeout(Duration.ofMinutes(5))
            .build()
    )

    override fun processImageVariants(params: ImageVariantWorkflowParams): List<String> {
        MDC.put("image_variant_set_id", params.imageVariantSetId)
        val results = mutableListOf<String>()

        // Process each variant using deterministic keys
        val sortedVariants = params.variants.sortedBy { it.hashCode() }

        for (variant in sortedVariants) {
            val activityParams = ProcessImageVariantParams(
                imageVariantSetId = params.imageVariantSetId,
                variantConfig = variant
            )

            val attachmentId = imageProcessor.processImageVariant(activityParams)

            results.add(attachmentId)
        }

        logger.info("processed ${results.size} image variants")

        return results
    }
}

data class ImageVariantWorkflowParams(
    @JsonProperty("imageVariantSetId") val imageVariantSetId: String,
    @JsonProperty("fieldName") val fieldName: String,
    @JsonProperty("variants") val variants: List<ImageVariantConfig>
)
