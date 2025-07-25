package net.cyclingbits.claudecode.api

import net.cyclingbits.claudecode.types.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClaudeCodeRequestTest {
    
    @Test
    fun `builder should create request with all fields`() {
        val request = ClaudeCodeRequest.builder()
            .prompt("Test prompt")
            .systemPrompt("System prompt")
            .allowedTools(listOf("read", "write"))
            .maxThinkingTokens(5000)
            .permissionMode(PermissionMode.ACCEPT_EDITS)
            .model("claude-3-opus-20240229")
            .maxTurns(10)
            .timeoutMs(60_000)
            .cwd(Paths.get("/tmp"))
            .build()
        
        assertEquals("Test prompt", request.prompt)
        assertNotNull(request.options)
        assertEquals("System prompt", request.options.systemPrompt)
        assertEquals(listOf("read", "write"), request.options.allowedTools)
        assertEquals(5000, request.options.maxThinkingTokens)
        assertEquals(PermissionMode.ACCEPT_EDITS, request.options.permissionMode)
        assertEquals("claude-3-opus-20240229", request.options.model)
        assertEquals(10, request.options.maxTurns)
        assertEquals(60_000, request.options.timeoutMs)
        assertEquals(Paths.get("/tmp"), request.options.cwd)
    }
    
    @Test
    fun `builder should work with minimal configuration`() {
        val request = ClaudeCodeRequest.builder()
            .prompt("Simple prompt")
            .build()
        
        assertEquals("Simple prompt", request.prompt)
        assertNotNull(request.options)
        // Check defaults
        assertEquals(emptyList(), request.options.allowedTools)
        assertEquals(8000, request.options.maxThinkingTokens)
        assertNull(request.options.systemPrompt)
        assertEquals(300_000, request.options.timeoutMs)
    }
    
    @Test
    fun `builder should throw IllegalStateException if prompt is empty`() {
        assertThrows<IllegalStateException> {
            ClaudeCodeRequest.builder().build()
        }
        
        assertThrows<IllegalStateException> {
            ClaudeCodeRequest.builder()
                .prompt("")
                .build()
        }
    }
    
    @Test
    fun `builder should support additional options`() {
        val request = ClaudeCodeRequest.builder()
            .prompt("Advanced prompt")
            .appendSystemPrompt("Additional context")
            .continueConversation(true)
            .resume("resume-token")
            .disallowedTools(listOf("dangerous_tool"))
            .permissionPromptToolName("custom_permission")
            .build()
        
        assertEquals("Advanced prompt", request.prompt)
        assertEquals("Additional context", request.options.appendSystemPrompt)
        assertEquals(true, request.options.continueConversation)
        assertEquals("resume-token", request.options.resume)
        assertEquals(listOf("dangerous_tool"), request.options.disallowedTools)
        assertEquals("custom_permission", request.options.permissionPromptToolName)
    }
    
    @Test
    fun `data class should support copy`() {
        val original = ClaudeCodeRequest(
            prompt = "Original prompt",
            options = ClaudeCodeOptions(systemPrompt = "Original system")
        )
        
        val copied = original.copy(prompt = "Modified prompt")
        
        assertEquals("Modified prompt", copied.prompt)
        assertEquals("Original system", copied.options.systemPrompt)
        assertEquals("Original prompt", original.prompt) // Original unchanged
    }
    
    @Test
    fun `builder should be fluent`() {
        // Test that all builder methods return the builder instance
        val builder = ClaudeCodeRequest.builder()
        
        val sameBuilder = builder
            .prompt("Test")
            .allowedTools(listOf("tool"))
            .maxThinkingTokens(1000)
            .systemPrompt("System")
            .appendSystemPrompt("Append")
            .mcpTools(listOf("mcp"))
            .mcpServers(emptyMap())
            .permissionMode(PermissionMode.ACCEPT_EDITS)
            .continueConversation(false)
            .resume("token")
            .maxTurns(5)
            .disallowedTools(listOf("bad"))
            .model("model")
            .permissionPromptToolName("perm")
            .cwd(Paths.get("/"))
            .timeoutMs(10000)
        
        // All methods should return the same builder instance
        assertEquals(builder, sameBuilder)
    }
    
    @Test
    fun `builder should support MCP configuration`() {
        val mcpServers = mapOf(
            "filesystem" to McpStdioServerConfig(
                type = "stdio",
                command = "npx",
                args = listOf("@modelcontextprotocol/server-filesystem", "/path"),
                env = mapOf("DEBUG" to "1")
            ),
            "github" to McpStdioServerConfig(
                type = "stdio",
                command = "npx",
                args = listOf("@modelcontextprotocol/server-github"),
                env = mapOf("GITHUB_TOKEN" to "token123")
            )
        )
        
        val mcpTools = listOf("mcp__filesystem__read", "mcp__github__search")
        
        val request = ClaudeCodeRequest.builder()
            .prompt("List files")
            .mcpServers(mcpServers)
            .mcpTools(mcpTools)
            .build()
        
        assertEquals(mcpServers, request.options.mcpServers)
        assertEquals(mcpTools, request.options.mcpTools)
        assertEquals(2, request.options.mcpServers.size)
        assertTrue(request.options.mcpServers.containsKey("filesystem"))
        assertTrue(request.options.mcpServers.containsKey("github"))
    }
    
    @Test
    fun `builder should support different MCP server types`() {
        val mcpServers = mapOf(
            "stdio-server" to McpStdioServerConfig(
                type = "stdio",
                command = "test-cmd",
                args = listOf("arg1"),
                env = mapOf("KEY" to "value")
            ),
            "sse-server" to McpSSEServerConfig(
                type = "sse",
                url = "https://sse.example.com",
                headers = mapOf("Authorization" to "Bearer token")
            ),
            "http-server" to McpHttpServerConfig(
                type = "http",
                url = "https://api.example.com",
                headers = mapOf("API-Key" to "key123")
            )
        )
        
        val request = ClaudeCodeRequest.builder()
            .prompt("Test")
            .mcpServers(mcpServers)
            .build()
        
        assertEquals(3, request.options.mcpServers.size)
        
        val stdioServer = request.options.mcpServers["stdio-server"] as McpStdioServerConfig
        assertEquals("test-cmd", stdioServer.command)
        assertEquals(listOf("arg1"), stdioServer.args)
        
        val sseServer = request.options.mcpServers["sse-server"] as McpSSEServerConfig
        assertEquals("https://sse.example.com", sseServer.url)
        
        val httpServer = request.options.mcpServers["http-server"] as McpHttpServerConfig
        assertEquals("https://api.example.com", httpServer.url)
    }
}