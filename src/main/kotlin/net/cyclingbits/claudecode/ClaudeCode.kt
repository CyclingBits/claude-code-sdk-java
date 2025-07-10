@file:JvmName("ClaudeCode")
package net.cyclingbits.claudecode

import net.cyclingbits.claudecode.api.ClaudeCodeClient
import net.cyclingbits.claudecode.types.*
import net.cyclingbits.claudecode.exceptions.*

/**
 * Claude Code SDK version.
 */
public const val VERSION: String = "0.1.0-SNAPSHOT"

/**
 * Creates a new ClaudeCodeClient instance.
 * 
 * This is a convenience function for creating the client.
 * 
 * @return A new ClaudeCodeClient instance
 */
@JvmName("createClient")
public fun claudeCodeClient(): ClaudeCodeClient = ClaudeCodeClient()

// Re-export main types for convenience
public typealias ClaudeCodeClient = net.cyclingbits.claudecode.api.ClaudeCodeClient
public typealias ClaudeCodeOptions = net.cyclingbits.claudecode.types.ClaudeCodeOptions
public typealias PermissionMode = net.cyclingbits.claudecode.types.PermissionMode
public typealias Message = net.cyclingbits.claudecode.types.Message
public typealias UserMessage = net.cyclingbits.claudecode.types.UserMessage
public typealias AssistantMessage = net.cyclingbits.claudecode.types.AssistantMessage
public typealias SystemMessage = net.cyclingbits.claudecode.types.SystemMessage
public typealias ResultMessage = net.cyclingbits.claudecode.types.ResultMessage
public typealias ContentBlock = net.cyclingbits.claudecode.types.ContentBlock
public typealias TextBlock = net.cyclingbits.claudecode.types.TextBlock
public typealias ToolUseBlock = net.cyclingbits.claudecode.types.ToolUseBlock
public typealias ToolResultBlock = net.cyclingbits.claudecode.types.ToolResultBlock