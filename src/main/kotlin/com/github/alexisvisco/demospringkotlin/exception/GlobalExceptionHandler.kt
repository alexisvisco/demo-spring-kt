package com.github.alexisvisco.demospringkotlin.exception

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.LocalDateTime


interface BusinessException {
    val errorCode: String
        get() = this::class.simpleName ?: "UnknownError"
}

abstract class AppException(
    message: String,
    override val errorCode: String = ""
) : RuntimeException(message), BusinessException {
    fun getCode(): String = errorCode.ifEmpty {
        this::class.simpleName ?: "UnknownError"
    }
}


data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val code: String,
    val message: String? = null,
    val path: String? = null,
    val fieldErrors: Map<String, FieldError>? = null
)

data class FieldError(
    val code: String,
    val message: String
)

@RestControllerAdvice
class GlobalExceptionHandler {

    companion object {
        const val EXCEPTION_ATTRIBUTE = "CAPTURED_EXCEPTION"
    }

    @ExceptionHandler(AppException::class)
    fun handleAppException(
        ex: AppException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        storeExceptionInRequest(ex, request)
        val status = resolveHttpStatus(ex)

        return createErrorResponse(
            status = status,
            code = ex.getCode(),
            message = ex.message,
            request = request
        )
    }

    private fun resolveHttpStatus(ex: Exception): HttpStatus {
        val responseStatus = AnnotationUtils.findAnnotation(ex.javaClass, ResponseStatus::class.java)
        return responseStatus?.value ?: HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAuthorizationDenied(
        ex: AuthorizationDeniedException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        return createErrorResponse(
            status = HttpStatus.FORBIDDEN,
            code = "AUTHORIZATION_DENIED",
            message = ex.message,
            request = request
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate { fieldError ->
            fieldError.field to FieldError(
                code = getValidationErrorCode(fieldError),
                message = fieldError.defaultMessage ?: "Invalid value"
            )
        }

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            code = "VALIDATION_ERROR",
            message = "Validation failed for ${fieldErrors.size} field(s)",
            path = request.getDescription(false).replace("uri=", ""),
            fieldErrors = fieldErrors
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.constraintViolations.associate { violation ->
            val fieldName = violation.propertyPath.toString().split(".").last()
            fieldName to FieldError(
                code = getConstraintErrorCode(violation.constraintDescriptor.annotation.annotationClass.simpleName),
                message = violation.message
            )
        }

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            code = "CONSTRAINT_VIOLATION",
            message = "Constraint violation on ${fieldErrors.size} field(s)",
            path = request.getDescription(false).replace("uri=", ""),
            fieldErrors = fieldErrors
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(
        ex: MissingServletRequestParameterException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            code = "MISSING_PARAMETER",
            message = "Required parameter '${ex.parameterName}' is missing",
            path = request.getDescription(false).replace("uri=", ""),
            fieldErrors = mapOf(
                ex.parameterName to FieldError(
                    code = "Required",
                    message = "This parameter is required"
                )
            )
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val expectedType = ex.requiredType?.simpleName ?: "unknown"
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            code = "TYPE_MISMATCH",
            message = "Failed to convert parameter '${ex.name}' to type $expectedType",
            path = request.getDescription(false).replace("uri=", ""),
            fieldErrors = mapOf(
                ex.name to FieldError(
                    code = "TypeMismatch",
                    message = "Expected type: $expectedType"
                )
            )
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }


    @ExceptionHandler(Exception::class)
    fun handleGeneral(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        storeExceptionInRequest(ex, request)
        return createErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            code = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred",
            request = request
        )
    }

    private fun storeExceptionInRequest(ex: Exception, request: WebRequest) {
        if (request is ServletWebRequest) {
            request.request.setAttribute(EXCEPTION_ATTRIBUTE, ex)
        }
    }

    private fun createErrorResponse(
        status: HttpStatus,
        code: String,
        message: String?,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            code = code,
            message = message,
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity.status(status).body(errorResponse)
    }

    private fun getValidationErrorCode(fieldError: org.springframework.validation.FieldError): String {
        return when (fieldError.code) {
            "NotBlank", "NotNull", "NotEmpty" -> "Required"
            "Email" -> "InvalidEmail"
            "Size" -> "InvalidSize"
            "Min" -> "TooSmall"
            "Max" -> "TooLarge"
            "Pattern" -> "InvalidFormat"
            "Past" -> "MustBePast"
            "Future" -> "MustBeFuture"
            "Positive" -> "MustBePositive"
            "Negative" -> "MustBeNegative"
            else -> fieldError.code ?: "Invalid"
        }
    }

    private fun getConstraintErrorCode(annotationName: String?): String {
        return when (annotationName) {
            "NotBlank", "NotNull", "NotEmpty" -> "Required"
            "Email" -> "InvalidEmail"
            "Size" -> "InvalidSize"
            "Min" -> "TooSmall"
            "Max" -> "TooLarge"
            "Pattern" -> "InvalidFormat"
            "Past" -> "MustBePast"
            "Future" -> "MustBeFuture"
            "Positive" -> "MustBePositive"
            "Negative" -> "MustBeNegative"
            else -> annotationName ?: "Invalid"
        }
    }
}
