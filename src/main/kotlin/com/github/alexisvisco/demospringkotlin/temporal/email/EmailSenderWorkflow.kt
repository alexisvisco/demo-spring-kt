package com.github.alexisvisco.demospringkotlin.temporal.email

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.alexisvisco.demospringkotlin.service.SendEmailParams
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.spring.boot.WorkflowImpl
import io.temporal.workflow.Workflow
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import org.slf4j.MDC
import java.time.Duration

@WorkflowInterface
interface EmailSenderWorkflow {
    @WorkflowMethod
    fun send(params: EmailSenderWorkflowParams)
}


@WorkflowImpl(taskQueues = ["default"])
class EmailSenderWorkflowImpl : EmailSenderWorkflow {

    companion object {
        fun buildWorkflowId(userId: String): String {
            return "email-sender-workflow-$userId-${System.currentTimeMillis()}"
        }
    }

    private val logger = Workflow.getLogger(EmailSenderWorkflowImpl::class.java)

    private val emailSender = Workflow.newActivityStub(
        EmailSenderActivity::class.java,
        ActivityOptions.newBuilder()
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .build()
            )
            .setStartToCloseTimeout(Duration.ofSeconds(10)).build()
    )

    override fun send(params: EmailSenderWorkflowParams) {
        MDC.put("invocation_from", params.invocationFrom)

        emailSender.send(
            params.emailParams
        )

        logger.info("verification email sent")
    }
}


data class EmailSenderWorkflowParams(
    @JsonProperty("emailParams") val emailParams: SendEmailParams,
    @JsonProperty("invocationFrom") val invocationFrom: String
)
