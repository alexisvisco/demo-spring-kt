package com.github.alexisvisco.demospringkotlin.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus


@ResponseStatus(HttpStatus.NOT_FOUND)
class UserNotFoundException(
    message: String = "User not found"
) : AppException(message, "USER_NOT_FOUND")

@ResponseStatus(HttpStatus.CONFLICT)
class UserAlreadyExistsException(
    message: String = "User already exists"
) : AppException(message, "USER_ALREADY_EXISTS")

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UserUnauthorizedException(
    message: String = "Unauthorized"
) : AppException(message, "UNAUTHORIZED")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidAvatarSizeException(
    message: String = "Invalid avatar size"
) : AppException(message, "INVALID_AVATAR_SIZE")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidAvatarTypeException(
    message: String = "Invalid avatar type, only images are allowed (png, jpg, webp)"
) : AppException(message, "INVALID_AVATAR_TYPE")
