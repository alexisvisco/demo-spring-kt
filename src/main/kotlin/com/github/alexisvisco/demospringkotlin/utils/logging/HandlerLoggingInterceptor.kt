package com.github.alexisvisco.demospringkotlin.utils.logging

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class HandlerLoggingInterceptor : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (handler is HandlerMethod) {
            val controllerClass = handler.beanType.simpleName
            val methodName = handler.method.name
            
            MDC.put("controller", controllerClass)
            MDC.put("handler", methodName)
        }
        return true
    }
}