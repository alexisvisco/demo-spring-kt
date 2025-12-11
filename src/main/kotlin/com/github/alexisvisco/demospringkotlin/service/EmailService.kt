package com.github.alexisvisco.demospringkotlin.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.alexisvisco.demospringkotlin.temporal.email.EmailSenderWorkflow
import com.github.alexisvisco.demospringkotlin.temporal.email.EmailSenderWorkflowImpl
import com.github.alexisvisco.demospringkotlin.temporal.email.EmailSenderWorkflowParams
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import org.slf4j.MDC
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val wc: WorkflowClient,
) {

    companion object {
        private const val FROM_ADDRESS = "noreply@demospringkotlin.com"
    }

    fun send(emailServiceParams: SendEmailParams, userId: String, invocationFrom: String = "EmailService.send") {
        val b = WorkflowOptions.newBuilder()
            .setTaskQueue("default")
            .setWorkflowId(EmailSenderWorkflowImpl.buildWorkflowId(userId))
            .build()

        MDC.put("created_workflow_id", b.workflowId)

        val workflow = wc.newWorkflowStub(EmailSenderWorkflow::class.java, b)
        WorkflowClient.start(
            workflow::send, EmailSenderWorkflowParams(
                emailParams = emailServiceParams,
                invocationFrom = invocationFrom
            )
        )
    }

    fun sendSync(emailServiceParams: SendEmailParams) {
        if (emailServiceParams.bodyHtml != null) {
            val mimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")
            helper.setFrom(FROM_ADDRESS)
            helper.setTo(emailServiceParams.to)
            helper.setSubject(emailServiceParams.subject)
            helper.setText(emailServiceParams.bodyText, emailServiceParams.bodyHtml)
            mailSender.send(mimeMessage)
        } else {
            val message = SimpleMailMessage()
            message.setFrom(FROM_ADDRESS)
            message.setTo(emailServiceParams.to)
            message.setSubject(emailServiceParams.subject)
            message.setText(emailServiceParams.bodyText)
            mailSender.send(message)
        }
    }
}

data class SendEmailParams(
    @JsonProperty("to") val to: String,
    @JsonProperty("subject") val subject: String,
    @JsonProperty("bodyText") val bodyText: String,
    @JsonProperty("bodyHtml") val bodyHtml: String?,
)
