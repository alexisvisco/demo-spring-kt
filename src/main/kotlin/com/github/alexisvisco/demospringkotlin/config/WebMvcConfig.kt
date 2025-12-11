package com.github.alexisvisco.demospringkotlin.config

import com.github.alexisvisco.demospringkotlin.utils.logging.HandlerLoggingInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val handlerLoggingInterceptor: HandlerLoggingInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(handlerLoggingInterceptor)
    }
}
