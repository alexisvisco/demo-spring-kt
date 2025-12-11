package com.github.alexisvisco.demospringkotlin.config

import com.github.alexisvisco.demospringkotlin.security.TokenAuthenticationFilter
import com.github.alexisvisco.demospringkotlin.security.TokenAuthenticationProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val tokenAuthenticationProvider: TokenAuthenticationProvider
) {

    @Bean
    fun authenticationManager(): AuthenticationManager {
        return ProviderManager(listOf(tokenAuthenticationProvider))
    }

    @Bean
    fun tokenAuthenticationFilter(): TokenAuthenticationFilter {
        return TokenAuthenticationFilter(authenticationManager())
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(tokenAuthenticationFilter(), BasicAuthenticationFilter::class.java)
            .csrf { it.disable() }

        return http.build()
    }
}
