package com.github.alexisvisco.demospringkotlin.config

import com.p6spy.engine.spy.appender.MessageFormattingStrategy
import com.p6spy.engine.spy.appender.P6Logger
import com.p6spy.engine.logging.Category

/**
 * Custom P6Spy logger that integrates with our SqlInspector
 */
class P6SpyLogger : MessageFormattingStrategy {

    private val isDev: Boolean by lazy {
        System.getProperty("spring.profiles.active")?.contains("dev") == true ||
                System.getenv("SPRING_PROFILES_ACTIVE")?.contains("dev") == true
    }

    companion object {
        const val RESET = "\u001B[0m"
        const val CYAN = "\u001B[36m"
        const val YELLOW = "\u001B[33m"
        const val GREEN = "\u001B[32m"
        const val BLUE = "\u001B[34m"
        const val ORANGE = "\u001B[38;5;208m"
        const val RED = "\u001B[31m"
    }

    override fun formatMessage(
        connectionId: Int,
        now: String,
        elapsed: Long,
        category: String,
        prepared: String,
        sql: String,
        url: String
    ): String {
        if (!isDev || sql.trim().isEmpty()) {
            return ""
        }

        val stackTrace = Thread.currentThread().stackTrace

        // Find app frames
        val appFrames = stackTrace.filter { frame ->
            val className = frame.className
            className.startsWith("com.github.alexisvisco") &&
                    !className.contains("P6Spy") &&
                    !className.contains("SqlInspector") &&
                    !className.contains("$$") &&
                    !className.contains("CGLIB")
        }

        val origin = appFrames.firstOrNull()?.let {
            "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}"
        } ?: "unknown"

        // Clean SQL
        val cleanSql = sql.trim()
            .replace(Regex("/\\*.*?\\*/"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val coloredElapsed = when {
            elapsed < 50 -> "$CYAN${elapsed}ms$RESET"
            elapsed < 200 -> "$YELLOW${elapsed} ms$RESET"
            else -> "$RED${elapsed} ms$RESET"
        }

        // Don't print here, let the appender handle it
        return "$CYAN[SQL $coloredElapsed]$RESET $YELLOW$origin →$RESET $GREEN$cleanSql$RESET"
    }
}

class P6SpyAppender : P6Logger {
    override fun logSQL(
        connectionId: Int,
        now: String,
        elapsed: Long,
        category: Category,
        prepared: String,
        sql: String,
        url: String
    ) {
        val formatter = P6SpyLogger()
        val message = formatter.formatMessage(connectionId, now, elapsed, category.name, prepared, sql, url)
        if (message.isNotEmpty()) {
            println(message)
        }
    }

    override fun logException(e: Exception) {
        System.err.println("P6Spy Exception: ${e.message}")
    }

    override fun logText(text: String) {
        println(text)
    }

    override fun isCategoryEnabled(category: Category): Boolean = true
}
