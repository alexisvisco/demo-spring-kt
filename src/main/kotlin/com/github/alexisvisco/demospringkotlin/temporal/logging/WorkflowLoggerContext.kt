package com.github.alexisvisco.demospringkotlin.temporal.logging

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Context for adding custom attributes to the canonical log line within Temporal workflows and activities.
 * This class is designed to be serializable by Temporal.
 */
data class WorkflowLoggerContext(
    val id: String = UUID.randomUUID().toString(),
    private val attrs: ConcurrentHashMap<String, Any?> = ConcurrentHashMap(),
    var level: Level = Level.INFO
) {
    enum class Level {
        INFO, ERROR
    }

    /**
     * Add a key-value pair to the log line.
     */
    fun add(key: String, value: Any?) {
        attrs[key] = value
    }

    /**
     * Add an error to the log line.
     * This will set the log level to ERROR and include error details.
     */
    fun error(throwable: Throwable) {
        level = Level.ERROR
        attrs["error"] = throwable.message
        attrs["error_type"] = throwable.javaClass.simpleName
    }

    /**
     * Returns a read-only map of the current attributes.
     */
    fun getAttributes(): Map<String, Any?> = attrs.toMap()
}
