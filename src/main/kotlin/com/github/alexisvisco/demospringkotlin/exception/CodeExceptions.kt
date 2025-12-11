package com.github.alexisvisco.demospringkotlin.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class CodeNotFoundException(
    message: String = "Verification code not found"
) : AppException(message, "CODE_NOT_FOUND")
