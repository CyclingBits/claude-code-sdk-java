@file:JvmName("ClaudeCodeKt")
package net.cyclingbits.claudecode

import net.cyclingbits.claudecode.api.ClaudeCodeClient
import net.cyclingbits.claudecode.types.*
import net.cyclingbits.claudecode.exceptions.*
import net.cyclingbits.claudecode.types.AssistantMessage
import net.cyclingbits.claudecode.types.ClaudeCodeOptions
import net.cyclingbits.claudecode.types.ContentBlock
import net.cyclingbits.claudecode.types.Message
import net.cyclingbits.claudecode.types.PermissionMode
import net.cyclingbits.claudecode.types.ResultMessage
import net.cyclingbits.claudecode.types.SystemMessage
import net.cyclingbits.claudecode.types.TextBlock
import net.cyclingbits.claudecode.types.ToolResultBlock
import net.cyclingbits.claudecode.types.ToolUseBlock
import net.cyclingbits.claudecode.types.UserMessage
import java.nio.file.Path

/**
 * Claude Code SDK version.
 */
public const val VERSION: String = "0.1.0"

/**
 * Factory object for creating Claude Code SDK instances.
 * 
 * Provides static methods for Java interoperability.
 */
public object ClaudeCode {
    /**
     * Creates a new ClaudeCodeClient instance.
     * 
     * This is a convenience function for creating the client.
     * 
     * Example (Java):
     * ```java
     * ClaudeCodeClient client = ClaudeCode.createClient();
     * ```
     * 
     * @param cliPath Path to the Claude CLI executable (optional)
     * @return A new ClaudeCodeClient instance
     */
    @JvmStatic
    @JvmOverloads
    public fun createClient(cliPath: Path? = null): ClaudeCodeClient = ClaudeCodeClient(cliPath)
}

/**
 * Creates a new ClaudeCodeClient instance.
 * 
 * This is a convenience function for creating the client in Kotlin.
 * 
 * @return A new ClaudeCodeClient instance
 */
public fun claudeCodeClient(): ClaudeCodeClient = ClaudeCodeClient()

// Re-export main types for convenience
public typealias ClaudeCodeClient = net.cyclingbits.claudecode.api.ClaudeCodeClient
public typealias ClaudeCodeOptions = ClaudeCodeOptions
public typealias PermissionMode = PermissionMode
public typealias Message = Message
public typealias UserMessage = UserMessage
public typealias AssistantMessage = AssistantMessage
public typealias SystemMessage = SystemMessage
public typealias ResultMessage = ResultMessage
public typealias ContentBlock = ContentBlock
public typealias TextBlock = TextBlock
public typealias ToolUseBlock = ToolUseBlock
public typealias ToolResultBlock = ToolResultBlock