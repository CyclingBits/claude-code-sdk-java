@file:JvmName("ClaudeCodeKt")
package net.cyclingbits.claudecode

import net.cyclingbits.claudecode.api.ClaudeCodeClient
import java.nio.file.Path

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