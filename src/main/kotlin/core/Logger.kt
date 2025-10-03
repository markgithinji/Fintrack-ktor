package com.fintrack.core

import org.slf4j.LoggerFactory

inline fun <reified T> T.logger() = LoggerFactory.getLogger(T::class.java)!!

// Extension functions for convenient logging with lazy evaluation
fun org.slf4j.Logger.debug(lazyMessage: () -> String) {
    if (isDebugEnabled) debug(lazyMessage())
}

fun org.slf4j.Logger.info(lazyMessage: () -> String) {
    if (isInfoEnabled) info(lazyMessage())
}

fun org.slf4j.Logger.warn(lazyMessage: () -> String) {
    if (isWarnEnabled) warn(lazyMessage())
}

fun org.slf4j.Logger.error(lazyMessage: () -> String, exception: Throwable? = null) {
    if (isErrorEnabled) {
        if (exception != null) error(lazyMessage(), exception) else error(lazyMessage())
    }
}

// Structured logging helpers
fun org.slf4j.Logger.withContext(vararg context: Pair<String, Any?>): StructuredLogger {
    return StructuredLogger(this, context.toMap())
}

class StructuredLogger(private val logger: org.slf4j.Logger, private val context: Map<String, Any?>) {
    fun debug(message: String) = logger.debug { "$message ${formatContext()}" }
    fun info(message: String) = logger.info { "$message ${formatContext()}" }
    fun warn(message: String) = logger.warn { "$message ${formatContext()}" }
    fun error(message: String, exception: Throwable? = null) = logger.error({ "$message ${formatContext()}" }, exception)

    private fun formatContext(): String = context.entries
        .filter { it.value != null }
        .joinToString(" ") { "${it.key}=${it.value}" }
}