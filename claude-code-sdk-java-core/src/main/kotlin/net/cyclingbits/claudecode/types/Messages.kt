package net.cyclingbits.claudecode.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Base interface for all message types.
 */
@Serializable
public sealed interface Message

/**
 * User message.
 * 
 * @property content Message content
 */
@Serializable
@SerialName("user")
public data class UserMessage(
    public val content: String
) : Message

/**
 * Assistant message with content blocks.
 * 
 * @property content List of content blocks
 */
@Serializable
@SerialName("assistant")
public data class AssistantMessage(
    public val content: List<ContentBlock>
) : Message {
    /**
     * Get all text blocks from the message.
     */
    public val textBlocks: List<TextBlock>
        get() = content.filterIsInstance<TextBlock>()
    
    /**
     * Get all tool use blocks from the message.
     */
    public val toolUseBlocks: List<ToolUseBlock>
        get() = content.filterIsInstance<ToolUseBlock>()
    
    /**
     * Get all tool result blocks from the message.
     */
    public val toolResultBlocks: List<ToolResultBlock>
        get() = content.filterIsInstance<ToolResultBlock>()
    
    /**
     * Get concatenated text content from all text blocks.
     */
    public val text: String
        get() = textBlocks.joinToString("") { it.text }
}

/**
 * System message with metadata.
 * 
 * @property subtype System message subtype
 * @property data Additional data
 */
@Serializable
@SerialName("system")
public data class SystemMessage(
    public val subtype: String,
    public val data: JsonObject
) : Message

/**
 * Result message with cost and usage information.
 * 
 * @property subtype Result subtype
 * @property durationMs Total duration in milliseconds
 * @property durationApiMs API duration in milliseconds
 * @property isError Whether this represents an error
 * @property numTurns Number of conversation turns
 * @property sessionId Session identifier
 * @property totalCostUsd Total cost in USD (optional)
 * @property usage Usage information (optional)
 * @property result Result string (optional)
 */
@Serializable
@SerialName("result")
public data class ResultMessage(
    public val subtype: String,
    @SerialName("duration_ms")
    public val durationMs: Int,
    @SerialName("duration_api_ms")
    public val durationApiMs: Int,
    @SerialName("is_error")
    public val isError: Boolean,
    @SerialName("num_turns")
    public val numTurns: Int,
    @SerialName("session_id")
    public val sessionId: String,
    @SerialName("total_cost_usd")
    public val totalCostUsd: Double? = null,
    public val usage: JsonObject? = null,
    public val result: String? = null
) : Message {
    /**
     * Get input tokens from usage data.
     */
    public val inputTokens: Int?
        get() = usage?.get("input_tokens")?.toString()?.toIntOrNull()
    
    /**
     * Get output tokens from usage data.
     */
    public val outputTokens: Int?
        get() = usage?.get("output_tokens")?.toString()?.toIntOrNull()
    
    /**
     * Get total tokens from usage data.
     */
    public val totalTokens: Int?
        get() = usage?.get("total_tokens")?.toString()?.toIntOrNull()
}