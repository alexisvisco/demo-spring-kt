package com.github.alexisvisco.demospringkotlin.utils.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import net.logstash.logback.encoder.LogstashEncoder
import java.io.IOException

class SnakeCaseLogstashEncoder : LogstashEncoder() {

    @Throws(IOException::class)
    override fun encode(event: ILoggingEvent): ByteArray {
        // Transform the event's MDC properties to snake_case
        val originalEvent = TransformedLoggingEvent(event)
        return super.encode(originalEvent)
    }

    private class TransformedLoggingEvent(private val original: ILoggingEvent) : ILoggingEvent by original {

        override fun getMDCPropertyMap(): MutableMap<String, String>? {
            val originalMap = original.mdcPropertyMap ?: return null
            val transformedMap = mutableMapOf<String, String>()

            originalMap.forEach { (key, value) ->
                val snakeKey = key.toSnakeCase()
                transformedMap[snakeKey] = value
            }

            return transformedMap
        }

        private fun String.toSnakeCase(): String {
            if (isEmpty()) return this

            val result = StringBuilder()

            for (i in indices) {
                val char = this[i]

                when {
                    char.isUpperCase() -> {
                        // Add underscore before uppercase if:
                        // - not at start
                        // - previous char is lowercase or digit
                        // - OR next char is lowercase (handles acronyms like "XMLParser")
                        if (i > 0 && (
                                    this[i - 1].isLowerCase() ||
                                            this[i - 1].isDigit() ||
                                            (i < lastIndex && this[i + 1].isLowerCase() && this[i - 1].isUpperCase())
                                    )) {
                            result.append('_')
                        }
                        result.append(char.lowercaseChar())
                    }
                    char in setOf(' ', '-') -> {
                        if (result.isNotEmpty() && result.last() != '_') {
                            result.append('_')
                        }
                    }
                    else -> result.append(char)
                }
            }

            return result.toString()
        }
    }
}
