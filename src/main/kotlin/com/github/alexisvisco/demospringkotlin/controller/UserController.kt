package com.github.alexisvisco.demospringkotlin.controller

import com.github.alexisvisco.demospringkotlin.dto.UserDto
import com.github.alexisvisco.demospringkotlin.dto.toDto
import com.github.alexisvisco.demospringkotlin.model.User
import com.github.alexisvisco.demospringkotlin.service.UserService
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) {
    @PostMapping
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest
    ): UserDto {
        return userService.createUser(
            User(
                email = request.email,
                passwordHash = request.password,
            )
        ).toDto()
    }

    @PostMapping("/verify-email")
    fun verifyEmail(
        @Valid @RequestBody request: VerifyEmailRequest
    ): UserDto {
        val user = userService.verifyUserEmailCode(request.code)
        return user.toDto(user.id)
    }

    @PostMapping("/login")
    fun loginUser(
        @Valid @RequestBody request: LoginUserRequest
    ): UserDto {
        val user = userService.login(request.email, request.password)
        return user.toDto(user.id)
    }

    @GetMapping("/@me")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUser(@AuthenticationPrincipal user: User): UserDto {
        return user.toDto(user.id)
    }

    @PatchMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    fun updateAvatar(
        @AuthenticationPrincipal user: User,
        @RequestParam("file") file: MultipartFile,
    ): UserDto {
        val updatedUser = userService.updateUserAvatar(user, file)
        return updatedUser.toDto(updatedUser.id)
    }

    @PostMapping("/add-picture")
    @PreAuthorize("isAuthenticated()")
    fun addPicture(
        @AuthenticationPrincipal user: User,
        @RequestParam("file") file: MultipartFile,
    ): UserDto {
        val updatedUser = userService.addUserPicture(user, file)
        return updatedUser.toDto(updatedUser.id)
    }
}


data class CreateUserRequest(
    @field:Email
    val email: String,

    @field:Size(min = 2)
    val password: String
)

data class LoginUserRequest(
    @field:Email
    val email: String,

    @field:Size(min = 2)
    val password: String
)

data class VerifyEmailRequest(
    @field:Size(min = 6, max = 6)
    val code: String
)
