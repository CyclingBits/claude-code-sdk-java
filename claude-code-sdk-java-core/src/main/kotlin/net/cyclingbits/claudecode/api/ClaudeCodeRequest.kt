package net.cyclingbits.claudecode.api

import net.cyclingbits.claudecode.types.ClaudeCodeOptions
import net.cyclingbits.claudecode.types.McpServerConfig
import net.cyclingbits.claudecode.types.PermissionMode
import org.jetbrains.annotations.Nullable
import java.nio.file.Path

/**
 * Request object for Claude queries.
 * 
 * This provides a Java-friendly alternative to the Kotlin DSL.
 * 
 * Example usage (Java):
 * ```java
 * ClaudeCodeRequest request = ClaudeCodeRequest.builder()
 *     .prompt("Help me write a function")
 *     .allowedTools(Arrays.asList("read", "write"))
 *     .systemPrompt("You are a Java expert")
 *     .maxTurns(5)
 *     .build();
 * 
 * client.queryAsync(request);
 * ```
 * 
 * @property prompt The prompt to send to Claude
 * @property options Configuration options
 */
public data class ClaudeCodeRequest(
    public val prompt: String,
    public val options: ClaudeCodeOptions = ClaudeCodeOptions()
) {
    
    /**
     * Builder for creating ClaudeCodeRequest instances.
     * 
     * This provides a fluent API for Java users.
     */
    public class Builder {
        private var prompt: String = ""
        private var allowedTools: List<String> = emptyList()
        private var maxThinkingTokens: Int = 8000
        @Nullable private var systemPrompt: String? = null
        @Nullable private var appendSystemPrompt: String? = null
        private var mcpTools: List<String> = emptyList()
        private var mcpServers: Map<String, McpServerConfig> = emptyMap()
        @Nullable private var permissionMode: PermissionMode? = null
        private var continueConversation: Boolean = false
        @Nullable private var resume: String? = null
        @Nullable private var maxTurns: Int? = null
        private var disallowedTools: List<String> = emptyList()
        @Nullable private var model: String? = null
        @Nullable private var permissionPromptToolName: String? = null
        @Nullable private var cwd: Path? = null
        private var timeoutMs: Long = 300_000
        
        /**
         * Set the prompt for the request.
         */
        public fun prompt(prompt: String): Builder = apply { 
            this.prompt = prompt 
        }
        
        /**
         * Set allowed tools for the request.
         */
        public fun allowedTools(tools: List<String>): Builder = apply { 
            this.allowedTools = tools 
        }
        
        /**
         * Set maximum thinking tokens.
         */
        public fun maxThinkingTokens(tokens: Int): Builder = apply { 
            this.maxThinkingTokens = tokens 
        }
        
        /**
         * Set system prompt.
         */
        public fun systemPrompt(@Nullable prompt: String?): Builder = apply { 
            this.systemPrompt = prompt 
        }
        
        /**
         * Set additional system prompt to append.
         */
        public fun appendSystemPrompt(@Nullable prompt: String?): Builder = apply { 
            this.appendSystemPrompt = prompt 
        }
        
        /**
         * Set MCP tools.
         */
        public fun mcpTools(tools: List<String>): Builder = apply { 
            this.mcpTools = tools 
        }
        
        /**
         * Set MCP servers.
         */
        public fun mcpServers(servers: Map<String, McpServerConfig>): Builder = apply { 
            this.mcpServers = servers 
        }
        
        /**
         * Set permission mode.
         */
        public fun permissionMode(@Nullable mode: PermissionMode?): Builder = apply { 
            this.permissionMode = mode 
        }
        
        /**
         * Set whether to continue previous conversation.
         */
        public fun continueConversation(value: Boolean): Builder = apply { 
            this.continueConversation = value 
        }
        
        /**
         * Set resume token.
         */
        public fun resume(@Nullable token: String?): Builder = apply { 
            this.resume = token 
        }
        
        /**
         * Set maximum conversation turns.
         */
        public fun maxTurns(@Nullable turns: Int?): Builder = apply { 
            this.maxTurns = turns 
        }
        
        /**
         * Set disallowed tools.
         */
        public fun disallowedTools(tools: List<String>): Builder = apply { 
            this.disallowedTools = tools 
        }
        
        /**
         * Set model to use.
         */
        public fun model(@Nullable model: String?): Builder = apply { 
            this.model = model 
        }
        
        /**
         * Set permission prompt tool name.
         */
        public fun permissionPromptToolName(@Nullable name: String?): Builder = apply { 
            this.permissionPromptToolName = name 
        }
        
        /**
         * Set current working directory.
         */
        public fun cwd(@Nullable directory: Path?): Builder = apply { 
            this.cwd = directory 
        }
        
        /**
         * Set timeout in milliseconds.
         */
        public fun timeoutMs(timeout: Long): Builder = apply { 
            this.timeoutMs = timeout 
        }
        
        /**
         * Build the ClaudeCodeRequest.
         * 
         * @throws IllegalStateException if prompt is empty
         */
        public fun build(): ClaudeCodeRequest {
            if (prompt.isEmpty()) {
                throw IllegalStateException("Prompt must not be empty")
            }
            
            val options = ClaudeCodeOptions(
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
            
            return ClaudeCodeRequest(prompt, options)
        }
    }
    
    public companion object {
        /**
         * Create a new builder instance.
         */
        @JvmStatic
        public fun builder(): Builder = Builder()
    }
}