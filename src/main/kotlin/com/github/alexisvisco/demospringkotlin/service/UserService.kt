package com.github.alexisvisco.demospringkotlin.service

import com.github.alexisvisco.demospringkotlin.exception.CodeNotFoundException
import com.github.alexisvisco.demospringkotlin.exception.UserAlreadyExistsException
import com.github.alexisvisco.demospringkotlin.exception.UserNotFoundException
import com.github.alexisvisco.demospringkotlin.exception.UserUnauthorizedException
import com.github.alexisvisco.demospringkotlin.model.*
import com.github.alexisvisco.demospringkotlin.repository.AttachmentRepository
import com.github.alexisvisco.demospringkotlin.repository.CodeRepository
import com.github.alexisvisco.demospringkotlin.repository.UserRepository
import com.github.f4b6a3.uuid.UuidCreator
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.time.LocalDateTime
import kotlin.uuid.Uuid

@Service
class UserService(
    private val userRepository: UserRepository,
    private val codeRepository: CodeRepository,
    private val attachmentRepository: AttachmentRepository,
    private val storageService: StorageService,
    private val emailService: EmailService,
    private val imageVariantService: ImageVariantService
) {

    companion object {
        val passwordEncoder = BCryptPasswordEncoder()
        const val CONSTRAINT_VIOLATION_EMAIL_UNIQUE = "users_email_key"
    }

    @Transactional
    fun createUser(newUser: User): User {
        newUser.accessToken = generateAccessToken()
        newUser.passwordHash = passwordEncoder.encode(newUser.passwordHash).toString()

        val savedUser = try {
            val user = userRepository.save(newUser)
            userRepository.flush()
            user
        } catch (e: Exception) {
            when {
                e.message?.contains(CONSTRAINT_VIOLATION_EMAIL_UNIQUE) == true ->
                    throw UserAlreadyExistsException("Email already exists")

                else -> throw e
            }
        }


        val emailVerificationCode = Code(
            value = CodeUtils.generateRandomCode(),
            type = CodeType.EMAIL_VERIFICATION,
            ownerType = CodeOwnerType.USER,
            ownerId = savedUser.id!!,
        )
        val createdCode = codeRepository.save(emailVerificationCode)

        sendVerificationEmail(savedUser, createdCode.value)

        MDC.put("created_user_id", savedUser.id!!)
        MDC.put("email_verification_code", createdCode.value)

        return savedUser
    }

    @Transactional
    fun verifyUserEmailCode(code: String): User {
        val mayCode = codeRepository.findValidCode(
            type = CodeType.EMAIL_VERIFICATION,
            ownerType = CodeOwnerType.USER,
            value = code,
            now = LocalDateTime.now(),
        )

        if (mayCode == null) {
            throw CodeNotFoundException()
        }

        val user = userRepository.findById(mayCode.ownerId).orElseThrow { UserNotFoundException() }
        user.emailVerifiedAt = LocalDateTime.now()


        userRepository.save(user)
        codeRepository.delete(mayCode)

        MDC.put("verified_user_id", user.id!!)

        return user
    }

    fun login(email: String, password: String): User {
        val user = userRepository.findByEmail(email) ?: throw UserUnauthorizedException()

        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw UserUnauthorizedException()
        }

        MDC.put("logged_in_user_id", user.id!!)

        return user
    }

    private fun generateAccessToken(): String {
        return (UuidCreator.getTimeOrderedEpoch().toString() + UuidCreator.getTimeOrderedEpoch().toString())
            .replace("-", "")
    }

    private fun sendVerificationEmail(user: User, code: String) {
        val emailParams = SendEmailParams(
            to = user.email,
            subject = "Verify your email",
            bodyText = "Your code is $code",
            bodyHtml = "Your code is <strong>$code</strong>",
        )
        emailService.send(
            emailParams,
            user.id!!,
            "${UserService::class.simpleName}.sendVerificationEmail"
        )
    }

    @Transactional
    fun updateUserAvatar(user: User, file: MultipartFile): User {
        user.attachment?.let { oldAttachment ->
            storageService.deleteContent(oldAttachment)
            attachmentRepository.delete(oldAttachment)
        }

        val avatarRules = StorageService.FileValidationRules(
            maxSize = 7 * 1024 * 1024L, // 7MB
            allowedTypes = setOf("image/jpeg", "image/png", "image/webp")
        )

        val attachment = storageService.createAttachmentFromFile(file, avatarRules)
        storageService.setContent(attachment, "avatars", file.inputStream)
        val savedAttachment = attachmentRepository.save(attachment)

        user.attachment = savedAttachment
        val updatedUser = userRepository.save(user)

        MDC.put("uploaded_attachment_id", savedAttachment.id!!)

        return updatedUser
    }

    @Transactional
    fun addUserPicture(user: User, file: MultipartFile): User {
        val picturesField = User::pictures

        val imageVariantSet = imageVariantService.createImageVariantSet(
            file = file,
            field = picturesField,
            kindType = "User",
            kindId = user.id!!
        )

        // Reload user within transaction to avoid LazyInitializationException
        val managedUser = userRepository.findById(user.id!!).orElseThrow { UserNotFoundException() }
        managedUser.pictures.add(imageVariantSet)
        val updatedUser = userRepository.save(managedUser)

        return updatedUser
    }
}
