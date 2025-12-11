package com.github.alexisvisco.demospringkotlin.security

import com.github.alexisvisco.demospringkotlin.repository.UserRepository
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class TokenAuthenticationProvider(
    private val userRepository: UserRepository
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val token = authentication.credentials as String

        val user = userRepository.findByAccessToken(token)
            ?: throw BadCredentialsException("Invalid access token")

        if (!user.isEnabled) {
            throw BadCredentialsException("User account is not enabled")
        }

        if (!user.isAccountNonLocked) {
            throw BadCredentialsException("User account is locked")
        }

        if (!user.isAccountNonExpired) {
            throw BadCredentialsException("User account has expired")
        }

        if (!user.isCredentialsNonExpired) {
            throw BadCredentialsException("User credentials have expired")
        }

        return TokenAuthentication(token, user).apply {
            isAuthenticated = true
        }
    }

    override fun supports(authentication: Class<*>): Boolean {
        return TokenAuthentication::class.java.isAssignableFrom(authentication)
    }
}
