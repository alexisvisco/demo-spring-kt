package com.github.alexisvisco.demospringkotlin.service

import com.github.alexisvisco.demospringkotlin.dto.AttachmentDto
import com.github.alexisvisco.demospringkotlin.exception.InvalidAvatarSizeException
import com.github.alexisvisco.demospringkotlin.exception.InvalidAvatarTypeException
import com.github.alexisvisco.demospringkotlin.model.Attachment
import com.github.alexisvisco.demospringkotlin.utils.idgen.PrefixedUuidGenerator
import com.github.f4b6a3.uuid.UuidCreator
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.io.InputStream
import java.time.Duration

@Service
class StorageService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    @Value("\${S3_BUCKET:attachments}")
    private val bucketName: String?,
) {
    companion object {
        private val tika = Tika()

        fun generateAttachmentChecksum(file: InputStream): String {
            val bytes = file.readAllBytes()
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(bytes)
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }

    data class FileValidationRules(
        val maxSize: Long,
        val allowedTypes: Set<String>
    )

    fun createAttachmentFromFile(
        file: MultipartFile,
        validationRules: FileValidationRules
    ): Attachment {
        val detectedType = validateFile(file, validationRules)

        val attachment = Attachment(
            filename = file.originalFilename ?: "file_${UuidCreator.getTimeOrderedEpoch()}",
            contentType = detectedType,
            byteSize = file.size,
            key = "",
            checksum = ""
        )

        return attachment
    }

    private fun validateFile(file: MultipartFile, rules: FileValidationRules): String {
        if (file.isEmpty) {
            throw InvalidAvatarTypeException("File is empty")
        }

        if (file.size > rules.maxSize) {
            throw InvalidAvatarSizeException()
        }

        val detectedType = tika.detect(file.inputStream)
        if (detectedType !in rules.allowedTypes) {
            throw InvalidAvatarTypeException()
        }

        return detectedType
    }

    fun setContent(attachment: Attachment, folder: String, inputStream: InputStream) {
        try {
            val key = generateS3Key(folder, attachment)
            val bytes = inputStream.readAllBytes()

            attachment.key = key
            attachment.checksum = generateAttachmentChecksum(bytes.inputStream())

            val putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(attachment.contentType)
                .contentLength(bytes.size.toLong())
                .build()

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes))
        } catch (e: Exception) {
            throw RuntimeException("Failed to upload file to S3", e)
        }
    }

    fun getContent(attachment: Attachment): InputStream {
        try {

            val getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(attachment.key)
                .build()

            val responseInputStream = s3Client.getObject(getObjectRequest)
            return responseInputStream
        } catch (e: Exception) {
            throw RuntimeException("Failed to retrieve file from S3", e)
        }
    }

    fun deleteContent(attachment: Attachment) {
        try {
            val deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(attachment.key)
                .build()

            s3Client.deleteObject(deleteObjectRequest)
        } catch (e: Exception) {
            throw RuntimeException("Failed to delete file from S3", e)
        }
    }

    fun getPublicUrl(attachment: Attachment, expirationMinutes: Long = 60): String {
        try {
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(attachment.key)
                .build()

            val presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build()

            return s3Presigner.presignGetObject(presignRequest).url().toString()
        } catch (e: Exception) {
            throw RuntimeException("Failed to generate presigned URL for attachment", e)
        }
    }

    private fun generateS3Key(folder: String, att: Attachment): String {
        return "$folder/${UuidCreator.getTimeOrderedEpoch()}_${att.filename}"
    }
}
