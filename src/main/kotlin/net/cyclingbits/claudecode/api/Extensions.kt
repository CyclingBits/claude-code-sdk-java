@file:JvmName("ClaudeCodeExtensions")
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package net.cyclingbits.claudecode.api

import kotlinx.coroutines.flow.*
import net.cyclingbits.claudecode.types.*

/**
 * Extension functions for more convenient API usage.
 */

/**
 * Filter messages to only assistant messages.
 */
public fun Flow<Message>.assistantMessages(): Flow<AssistantMessage> =
    filterIsInstance<AssistantMessage>()

/**
 * Filter messages to only user messages.
 */
public fun Flow<Message>.userMessages(): Flow<UserMessage> =
    filterIsInstance<UserMessage>()

/**
 * Filter messages to only system messages.
 */
public fun Flow<Message>.systemMessages(): Flow<SystemMessage> =
    filterIsInstance<SystemMessage>()

/**
 * Filter messages to only result messages.
 */
public fun Flow<Message>.resultMessages(): Flow<ResultMessage> =
    filterIsInstance<ResultMessage>()

/**
 * Extract text content from assistant messages.
 */
public fun Flow<Message>.textContent(): Flow<String> =
    filterIsInstance<AssistantMessage>()
        .map { it.text }
        .filter { it.isNotEmpty() }

/**
 * Extract all tool uses from assistant messages.
 */
public fun Flow<Message>.toolUses(): Flow<ToolUseBlock> =
    filterIsInstance<AssistantMessage>()
        .flatMapConcat { message ->
            message.toolUseBlocks.asFlow()
        }

/**
 * Extract all tool results from assistant messages.
 */
public fun Flow<Message>.toolResults(): Flow<ToolResultBlock> =
    filterIsInstance<AssistantMessage>()
        .flatMapConcat { message ->
            message.toolResultBlocks.asFlow()
        }

/**
 * Get only non-error messages.
 */
public fun Flow<Message>.successfulMessages(): Flow<Message> =
    filter { message ->
        when (message) {
            is ResultMessage -> !message.isError
            else -> true
        }
    }

/**
 * Get only error messages.
 */
public fun Flow<Message>.errorMessages(): Flow<Message> =
    filter { message ->
        when (message) {
            is ResultMessage -> message.isError
            else -> false
        }
    }

/**
 * Convenience function to create a simple text-only assistant message.
 */
public fun assistantMessage(text: String): AssistantMessage =
    AssistantMessage(listOf(TextBlock(text)))

/**
 * Convenience function to create a user message.
 */
public fun userMessage(content: String): UserMessage =
    UserMessage(content)

/**
 * Operator function to combine messages.
 */
public operator fun AssistantMessage.plus(other: AssistantMessage): AssistantMessage =
    AssistantMessage(this.content + other.content)