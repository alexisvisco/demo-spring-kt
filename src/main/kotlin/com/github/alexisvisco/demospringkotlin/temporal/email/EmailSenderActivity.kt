package com.github.alexisvisco.demospringkotlin.temporal.email


import com.github.alexisvisco.demospringkotlin.service.EmailService
import com.github.alexisvisco.demospringkotlin.service.SendEmailParams
import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod
import io.temporal.spring.boot.ActivityImpl
import org.springframework.stereotype.Component

@ActivityInterface
interface EmailSenderActivity {
    @ActivityMethod
    fun send(params: SendEmailParams)
}


@Component
@ActivityImpl(taskQueues = ["default"])
class EmailSenderActivityImpl(private val emailService: EmailService) : EmailSenderActivity {
    override fun send(params: SendEmailParams) {
        emailService.sendSync(params)
    }
}
