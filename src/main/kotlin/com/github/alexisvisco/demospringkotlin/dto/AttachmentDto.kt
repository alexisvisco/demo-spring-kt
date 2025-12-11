package com.github.alexisvisco.demospringkotlin.dto

import com.github.alexisvisco.demospringkotlin.model.Attachment
import com.github.alexisvisco.demospringkotlin.service.StorageService
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

data class AttachmentDto(
    val id: String,
    val contentType: String,
    val url: String
)

@Component
class AttachmentDtoMapper : ApplicationContextAware {
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        Companion.applicationContext = applicationContext
    }

    companion object {
        private var applicationContext: ApplicationContext? = null

        fun getStorageService(): StorageService {
            return applicationContext?.getBean(StorageService::class.java)
                ?: throw IllegalStateException("ApplicationContext not initialized")
        }
    }
}

fun Attachment.toDto(expirationMinutes: Long = 60): AttachmentDto {
    val storageService = AttachmentDtoMapper.getStorageService()
    return AttachmentDto(
        id = this.id!!,
        contentType = this.contentType,
        url = storageService.getPublicUrl(this, expirationMinutes)
    )
}
