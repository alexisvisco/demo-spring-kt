package com.github.alexisvisco.demospringkotlin.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.jboss.logging.MDC
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TokenAuthenticationFilter(
    private val authenticationManager: AuthenticationManager
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)

            try {
                val authentication = TokenAuthentication(token)
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                val authenticated = authenticationManager.authenticate(authentication)
                SecurityContextHolder.getContext().authentication = authenticated

                MDC.put("user_id", (authenticated.principal as? UserDetails)?.username)
            } catch (e: Exception) {
                // Token invalid or user not authorized
            }
        }

        filterChain.doFilter(request, response)
    }
}
