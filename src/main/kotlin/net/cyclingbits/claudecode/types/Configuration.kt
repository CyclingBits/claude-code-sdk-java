package net.cyclingbits.claudecode.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.nio.file.Path

/**
 * Permission mode for tool execution.
 */
@Serializable
public enum class PermissionMode {
    @SerialName("default")
    DEFAULT,
    
    @SerialName("acceptEdits")
    ACCEPT_EDITS,
    
    @SerialName("bypassPermissions")
    BYPASS_PERMISSIONS
}

/**
 * Base interface for MCP server configuration.
 */
@Serializable
public sealed interface McpServerConfig

/**
 * MCP stdio server configuration.
 * 
 * @property type Server type (optional for backwards compatibility)
 * @property command Command to execute
 * @property args Command arguments
 * @property env Environment variables
 */
@Serializable
@SerialName("stdio")
public data class McpStdioServerConfig(
    public val type: String? = "stdio",
    public val command: String,
    public val args: List<String> = emptyList(),
    public val env: Map<String, String> = emptyMap()
) : McpServerConfig

/**
 * MCP SSE server configuration.
 * 
 * @property type Server type
 * @property url Server URL
 * @property headers HTTP headers
 */
@Serializable
@SerialName("sse")
public data class McpSSEServerConfig(
    public val type: String = "sse",
    public val url: String,
    public val headers: Map<String, String> = emptyMap()
) : McpServerConfig

/**
 * MCP HTTP server configuration.
 * 
 * @property type Server type
 * @property url Server URL
 * @property headers HTTP headers
 */
@Serializable
@SerialName("http")
public data class McpHttpServerConfig(
    public val type: String = "http",
    public val url: String,
    public val headers: Map<String, String> = emptyMap()
) : McpServerConfig

/**
 * Query options for Claude SDK.
 * 
 * @property allowedTools List of allowed tool names
 * @property maxThinkingTokens Maximum thinking tokens (default: 8000)
 * @property systemPrompt System prompt to use
 * @property appendSystemPrompt Additional system prompt to append
 * @property mcpTools List of MCP tool names
 * @property mcpServers Map of MCP server configurations
 * @property permissionMode Permission mode for tool execution
 * @property continueConversation Whether to continue previous conversation
 * @property resume Resume token for conversation
 * @property maxTurns Maximum conversation turns
 * @property disallowedTools List of disallowed tool names
 * @property model Model to use
 * @property permissionPromptToolName Permission prompt tool name
 * @property cwd Working directory
 * @property timeoutMs Timeout in milliseconds for the query (default: 5 minutes)
 */
public data class ClaudeCodeOptions @JvmOverloads constructor(
    public val allowedTools: List<String> = emptyList(),
    public val maxThinkingTokens: Int = 8000,
    public val systemPrompt: String? = null,
    public val appendSystemPrompt: String? = null,
    public val mcpTools: List<String> = emptyList(),
    public val mcpServers: Map<String, McpServerConfig> = emptyMap(),
    public val permissionMode: PermissionMode? = null,
    public val continueConversation: Boolean = false,
    public val resume: String? = null,
    public val maxTurns: Int? = null,
    public val disallowedTools: List<String> = emptyList(),
    public val model: String? = null,
    public val permissionPromptToolName: String? = null,
    public val cwd: Path? = null,
    public val timeoutMs: Long = 300_000 // 5 minutes default
) {
    /**
     * Builder for Java interoperability.
     */
    public class Builder {
        private var allowedTools: List<String> = emptyList()
        private var maxThinkingTokens: Int = 8000
        private var systemPrompt: String? = null
        private var appendSystemPrompt: String? = null
        private var mcpTools: List<String> = emptyList()
        private var mcpServers: Map<String, McpServerConfig> = emptyMap()
        private var permissionMode: PermissionMode? = null
        private var continueConversation: Boolean = false
        private var resume: String? = null
        private var maxTurns: Int? = null
        private var disallowedTools: List<String> = emptyList()
        private var model: String? = null
        private var permissionPromptToolName: String? = null
        private var cwd: Path? = null
        private var timeoutMs: Long = 300_000

        public fun allowedTools(tools: List<String>): Builder = apply { this.allowedTools = tools }
        public fun maxThinkingTokens(tokens: Int): Builder = apply { this.maxThinkingTokens = tokens }
        public fun systemPrompt(prompt: String?): Builder = apply { this.systemPrompt = prompt }
        public fun appendSystemPrompt(prompt: String?): Builder = apply { this.appendSystemPrompt = prompt }
        public fun mcpTools(tools: List<String>): Builder = apply { this.mcpTools = tools }
        public fun mcpServers(servers: Map<String, McpServerConfig>): Builder = apply { this.mcpServers = servers }
        public fun permissionMode(mode: PermissionMode?): Builder = apply { this.permissionMode = mode }
        public fun continueConversation(value: Boolean): Builder = apply { this.continueConversation = value }
        public fun resume(token: String?): Builder = apply { this.resume = token }
        public fun maxTurns(turns: Int?): Builder = apply { this.maxTurns = turns }
        public fun disallowedTools(tools: List<String>): Builder = apply { this.disallowedTools = tools }
        public fun model(model: String?): Builder = apply { this.model = model }
        public fun permissionPromptToolName(name: String?): Builder = apply { this.permissionPromptToolName = name }
        public fun cwd(directory: Path?): Builder = apply { this.cwd = directory }
        public fun timeoutMs(timeout: Long): Builder = apply { this.timeoutMs = timeout }

        public fun build(): ClaudeCodeOptions = ClaudeCodeOptions(
            allowedTools = allowedTools,
            maxThinkingTokens = maxThinkingTokens,
            systemPrompt = systemPrompt,
            appendSystemPrompt = appendSystemPrompt,
            mcpTools = mcpTools,
            mcpServers = mcpServers,
            permissionMode = permissionMode,
            continueConversation = continueConversation,
            resume = resume,
            maxTurns = maxTurns,
            disallowedTools = disallowedTools,
            model = model,
            permissionPromptToolName = permissionPromptToolName,
            cwd = cwd,
            timeoutMs = timeoutMs
        )
    }

    public companion object {
        @JvmStatic
        public fun builder(): Builder = Builder()
    }
}