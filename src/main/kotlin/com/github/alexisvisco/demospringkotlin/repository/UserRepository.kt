package com.github.alexisvisco.demospringkotlin.repository

import com.github.alexisvisco.demospringkotlin.model.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, String> {
    @EntityGraph("User.full")
    fun findByEmail(email: String): User?

    @EntityGraph("User.full")
    fun findByAccessToken(accessToken: String): User?

    @EntityGraph("User.full")
    override fun findById(id: String): java.util.Optional<User>
}
