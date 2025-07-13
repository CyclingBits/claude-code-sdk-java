package net.cyclingbits.claudecode.types

import net.cyclingbits.claudecode.exceptions.ClaudeSDKException

/**
 * Sealed interface representing the outcome of a Claude query.
 * 
 * This provides a unified way to handle results in Java without multiple instanceof checks.
 * 
 * Example usage (Kotlin):
 * ```kotlin
 * when (outcome) {
 *     is Outcome.Success -> handleMessages(outcome.messages)
 *     is Outcome.Error -> handleError(outcome.exception)
 *     is Outcome.Timeout -> println("Query timed out after ${outcome.durationMs}ms")
 * }
 * ```
 * 
 * Example usage (Java 17+):
 * ```java
 * switch (outcome) {
 *     case Outcome.Success success -> handleMessages(success.getMessages());
 *     case Outcome.Error error -> handleError(error.getException());
 *     case Outcome.Timeout timeout -> System.out.println("Timed out after " + timeout.getDurationMs() + "ms");
 * }
 * ```
 * 
 * @param T The type of successful result
 */
public sealed interface Outcome<out T> {
    
    /**
     * Successful outcome containing the result.
     * 
     * @property value The successful result value
     * @property messages All messages received during the query
     * @property durationMs Total duration in milliseconds
     */
    public data class Success<T>(
        public val value: T,
        public val messages: List<Message>,
        public val durationMs: Long
    ) : Outcome<T>
    
    /**
     * Error outcome containing the exception.
     * 
     * @property exception The exception that occurred
     * @property partialMessages Any messages received before the error
     * @property durationMs Duration until error occurred
     */
    public data class Error(
        public val exception: ClaudeSDKException,
        public val partialMessages: List<Message> = emptyList(),
        public val durationMs: Long = 0
    ) : Outcome<Nothing>
    
    /**
     * Timeout outcome.
     * 
     * @property durationMs The timeout duration
     * @property partialMessages Any messages received before timeout
     */
    public data class Timeout(
        public val durationMs: Long,
        public val partialMessages: List<Message> = emptyList()
    ) : Outcome<Nothing>
}

/**
 * Extension functions for working with Outcome.
 */

/**
 * Transform a successful outcome value.
 */
public inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value), messages, durationMs)
    is Outcome.Error -> this
    is Outcome.Timeout -> this
}

/**
 * Get the value or null if not successful.
 */
public fun <T> Outcome<T>.getOrNull(): T? = when (this) {
    is Outcome.Success -> value
    else -> null
}

/**
 * Get the value or throw the exception.
 */
public fun <T> Outcome<T>.getOrThrow(): T = when (this) {
    is Outcome.Success -> value
    is Outcome.Error -> throw exception
    is Outcome.Timeout -> throw net.cyclingbits.claudecode.exceptions.TimeoutException(
        durationMs,
        "Query timed out after ${durationMs}ms"
    )
}

/**
 * Execute a block if the outcome is successful.
 */
public inline fun <T> Outcome<T>.onSuccess(block: (value: T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) {
        block(value)
    }
    return this
}

/**
 * Execute a block if the outcome is an error.
 */
public inline fun <T> Outcome<T>.onError(block: (exception: ClaudeSDKException) -> Unit): Outcome<T> {
    if (this is Outcome.Error) {
        block(exception)
    }
    return this
}

/**
 * Execute a block if the outcome is a timeout.
 */
public inline fun <T> Outcome<T>.onTimeout(block: (durationMs: Long) -> Unit): Outcome<T> {
    if (this is Outcome.Timeout) {
        block(durationMs)
    }
    return this
}