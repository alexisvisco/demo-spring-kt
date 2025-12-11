package com.github.alexisvisco.demospringkotlin.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class TokenAuthentication(
    private val token: String,
    private var user: UserDetails? = null
) : AbstractAuthenticationToken(user?.authorities ?: emptyList()) {

    init {
        super.setAuthenticated(user != null)
    }

    override fun getCredentials(): String = token

    override fun getPrincipal(): Any = user ?: token
}
