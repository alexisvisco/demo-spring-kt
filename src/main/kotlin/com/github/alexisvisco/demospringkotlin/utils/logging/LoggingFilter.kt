package com.github.alexisvisco.demospringkotlin.utils.logging

import com.github.alexisvisco.demospringkotlin.exception.GlobalExceptionHandler
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.StringWriter
import java.io.PrintWriter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class LoggingFilter : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(LoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val start = System.currentTimeMillis()
        val correlationId = request.getHeader("X-Correlation-ID") ?: UUID.randomUUID().toString()

        try {
            // Set up MDC for the request
            MDC.put("req_id", correlationId)
            MDC.put("method", request.method)
            MDC.put("path", request.requestURI)
            MDC.put("remote_addr", request.remoteAddr ?: "unknown")

            filterChain.doFilter(request, response)

            // Add response info to MDC and log
            val duration = System.currentTimeMillis() - start
            MDC.put("status_code", response.status.toString())
            MDC.put("duration_ms", duration.toString())

            // Check if an exception was captured by GlobalExceptionHandler
            val capturedException = request.getAttribute(GlobalExceptionHandler.EXCEPTION_ATTRIBUTE) as? Exception

            if (capturedException != null) {
                MDC.put("error_type", capturedException.javaClass.simpleName)
                MDC.put("error_message", capturedException.message ?: "No message")

                // Add full stacktrace to MDC
                val sw = StringWriter()
                capturedException.printStackTrace(PrintWriter(sw))
                MDC.put("stacktrace", sw.toString())

                println("\n=== Captured Exception Stacktrace ===\n${sw}\n")

                logger.error("http request", capturedException)
            } else if (response.status >= 500) {
                logger.error("http request - server error")
            } else if (response.status >= 400) {
                logger.warn("http request - client error")
            } else {
                logger.info("http request")
            }
        } finally {
            // Always clear MDC to prevent memory leaks in virtual threads
            MDC.clear()
        }
    }
}
