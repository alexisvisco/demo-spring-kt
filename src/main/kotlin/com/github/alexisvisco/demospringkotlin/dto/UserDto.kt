package com.github.alexisvisco.demospringkotlin.dto

import com.github.alexisvisco.demospringkotlin.model.User
import java.time.LocalDateTime

data class UserDto(
    val id: String,
    val email: String?,
    val accessToken: String?,
    val createdAt: LocalDateTime,
)

fun User.toDto(currentUserId: String? = null) = UserDto(
    id = this.id!!,
    email = if (this.id == currentUserId) this.email else null,
    accessToken = if (this.id == currentUserId) this.accessToken else null,
    createdAt = this.createdAt,
)
