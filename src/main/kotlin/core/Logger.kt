package com.fintrack.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Primary logger function
inline fun <reified T> logger(): Logger = LoggerFactory.getLogger(T::class.java)

// Extension functions for convenient logging with lazy evaluation
inline fun Logger.debug(lazyMessage: () -> String) {
    if (isDebugEnabled) debug(lazyMessage())
}

inline fun Logger.info(lazyMessage: () -> String) {
    if (isInfoEnabled) info(lazyMessage())
}

inline fun Logger.warn(lazyMessage: () -> String) {
    if (isWarnEnabled) warn(lazyMessage())
}

inline fun Logger.error(lazyMessage: () -> String, exception: Throwable? = null) {
    if (isErrorEnabled) {
        if (exception != null) error(lazyMessage(), exception) else error(lazyMessage())
    }
}

inline fun Logger.trace(lazyMessage: () -> String) {
    if (isTraceEnabled) trace(lazyMessage())
}

// Structured logging helpers
fun Logger.withContext(vararg context: Pair<String, Any?>): StructuredLogger {
    return StructuredLogger(this, context.toMap())
}

fun Logger.withContext(context: Map<String, Any?>): StructuredLogger {
    return StructuredLogger(this, context)
}

class StructuredLogger internal constructor(
    private val logger: Logger,
    private val context: Map<String, Any?>
) {
    fun debug(lazyMessage: () -> String) =
        logger.debug { "${lazyMessage()} ${context.formatContext()}" }

    fun info(lazyMessage: () -> String) =
        logger.info { "${lazyMessage()} ${context.formatContext()}" }

    fun warn(lazyMessage: () -> String) =
        logger.warn { "${lazyMessage()} ${context.formatContext()}" }

    fun error(lazyMessage: () -> String, exception: Throwable? = null) =
        logger.error({ "${lazyMessage()} ${context.formatContext()}" }, exception)

    fun trace(lazyMessage: () -> String) =
        logger.trace { "${lazyMessage()} ${context.formatContext()}" }
}

private fun Map<String, Any?>.formatContext(): String = entries
    .filter { it.value != null }
    .joinToString(" ") { "${it.key}=[${it.value}]" }

// Convenience function for creating loggers with custom names
fun logger(name: String): Logger = LoggerFactory.getLogger(name)