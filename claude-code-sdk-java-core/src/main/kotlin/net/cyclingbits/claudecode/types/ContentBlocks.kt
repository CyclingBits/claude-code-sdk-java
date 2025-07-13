package net.cyclingbits.claudecode.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Base interface for content blocks.
 */
@Serializable
public sealed interface ContentBlock

/**
 * Text content block.
 * 
 * @property text The text content
 */
@Serializable
@SerialName("text")
public data class TextBlock(
    public val text: String
) : ContentBlock

/**
 * Tool use content block.
 * 
 * @property id Tool use ID
 * @property name Tool name
 * @property input Tool input parameters
 */
@Serializable
@SerialName("tool_use")
public data class ToolUseBlock(
    public val id: String,
    public val name: String,
    public val input: Map<String, JsonElement>
) : ContentBlock

/**
 * Tool result content block.
 * 
 * @property toolUseId ID of the tool use this result corresponds to
 * @property content Result content (can be string or structured data)
 * @property isError Whether this represents an error
 */
@Serializable
@SerialName("tool_result")
public data class ToolResultBlock(
    @SerialName("tool_use_id")
    public val toolUseId: String,
    public val content: JsonElement? = null,
    @SerialName("is_error")
    public val isError: Boolean? = null
) : ContentBlock