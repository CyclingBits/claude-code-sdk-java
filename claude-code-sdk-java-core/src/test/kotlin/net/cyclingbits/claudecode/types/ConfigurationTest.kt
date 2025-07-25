package net.cyclingbits.claudecode.types

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigurationTest {
    
    @Test
    fun `ClaudeCodeOptions builder should work correctly`() {
        val options = ClaudeCodeOptions.builder()
            .allowedTools(listOf("read", "write"))
            .systemPrompt("You are a helpful assistant")
            .maxThinkingTokens(10000)
            .cwd(Paths.get("/tmp"))
            .timeoutMs(60_000)
            .build()
        
        assertEquals(listOf("read", "write"), options.allowedTools)
        assertEquals("You are a helpful assistant", options.systemPrompt)
        assertEquals(10000, options.maxThinkingTokens)
        assertNotNull(options.cwd)
        assertEquals(60_000, options.timeoutMs)
    }
    
    @Test
    fun `ClaudeCodeOptions should have correct defaults`() {
        val options = ClaudeCodeOptions()
        
        assertEquals(emptyList(), options.allowedTools)
        assertEquals(8000, options.maxThinkingTokens)
        assertEquals(false, options.continueConversation)
        assertEquals(null, options.systemPrompt)
        assertEquals(300_000, options.timeoutMs) // 5 minutes default
    }
    
    @Test
    fun `ClaudeCodeOptions builder should support all properties`() {
        val mcpServers = mapOf(
            "test-server" to McpStdioServerConfig(
                type = "stdio",
                command = "test-cmd",
                args = listOf("arg1", "arg2"),
                env = mapOf("KEY" to "VALUE")
            )
        )
        
        val options = ClaudeCodeOptions.builder()
            .allowedTools(listOf("read", "write"))
            .disallowedTools(listOf("delete"))
            .systemPrompt("System prompt")
            .appendSystemPrompt("Append prompt")
            .maxThinkingTokens(12000)
            .mcpTools(listOf("mcp_tool1", "mcp_tool2"))
            .mcpServers(mcpServers)
            .permissionMode(PermissionMode.BYPASS_PERMISSIONS)
            .continueConversation(true)
            .resume("session-123")
            .maxTurns(5)
            .model("claude-3")
            .cwd(Paths.get("/workspace"))
            .permissionPromptToolName("permission_tool")
            .timeoutMs(120_000)
            .build()
        
        assertEquals(listOf("read", "write"), options.allowedTools)
        assertEquals(listOf("delete"), options.disallowedTools)
        assertEquals("System prompt", options.systemPrompt)
        assertEquals("Append prompt", options.appendSystemPrompt)
        assertEquals(12000, options.maxThinkingTokens)
        assertEquals(listOf("mcp_tool1", "mcp_tool2"), options.mcpTools)
        assertEquals(mcpServers, options.mcpServers)
        assertEquals(PermissionMode.BYPASS_PERMISSIONS, options.permissionMode)
        assertTrue(options.continueConversation)
        assertEquals("session-123", options.resume)
        assertEquals(5, options.maxTurns)
        assertEquals("claude-3", options.model)
        assertEquals(Paths.get("/workspace"), options.cwd)
        assertEquals("permission_tool", options.permissionPromptToolName)
        assertEquals(120_000, options.timeoutMs)
    }
    
    @Test
    fun `PermissionMode enum should have correct values`() {
        // Test all enum values exist and can be accessed
        val defaultMode = PermissionMode.DEFAULT
        val acceptEditsMode = PermissionMode.ACCEPT_EDITS
        val bypassMode = PermissionMode.BYPASS_PERMISSIONS
        
        assertEquals("DEFAULT", defaultMode.name)
        assertEquals("ACCEPT_EDITS", acceptEditsMode.name)
        assertEquals("BYPASS_PERMISSIONS", bypassMode.name)
        
        // Test valueOf
        assertEquals(PermissionMode.DEFAULT, PermissionMode.valueOf("DEFAULT"))
        assertEquals(PermissionMode.ACCEPT_EDITS, PermissionMode.valueOf("ACCEPT_EDITS"))
        assertEquals(PermissionMode.BYPASS_PERMISSIONS, PermissionMode.valueOf("BYPASS_PERMISSIONS"))
        
        // Test that all values are covered
        val allValues = PermissionMode.values()
        assertEquals(3, allValues.size)
        assertTrue(allValues.contains(PermissionMode.DEFAULT))
        assertTrue(allValues.contains(PermissionMode.ACCEPT_EDITS))
        assertTrue(allValues.contains(PermissionMode.BYPASS_PERMISSIONS))
    }
    
    @Test
    fun `McpStdioServerConfig should store properties correctly`() {
        val config = McpStdioServerConfig(
            type = "stdio",
            command = "npx",
            args = listOf("@modelcontextprotocol/server-filesystem", "/path"),
            env = mapOf("API_KEY" to "secret", "DEBUG" to "true")
        )
        
        assertEquals("stdio", config.type)
        assertEquals("npx", config.command)
        assertEquals(listOf("@modelcontextprotocol/server-filesystem", "/path"), config.args)
        assertEquals(mapOf("API_KEY" to "secret", "DEBUG" to "true"), config.env)
    }
    
    @Test
    fun `McpStdioServerConfig should work with empty collections`() {
        val config = McpStdioServerConfig(
            type = "stdio",
            command = "test",
            args = emptyList(),
            env = emptyMap()
        )
        
        assertEquals("stdio", config.type)
        assertEquals("test", config.command)
        assertTrue(config.args.isEmpty())
        assertTrue(config.env.isEmpty())
    }
    
    @Test
    fun `McpSSEServerConfig should store properties correctly`() {
        val config = McpSSEServerConfig(
            type = "sse",
            url = "https://api.example.com/sse",
            headers = mapOf(
                "Authorization" to "Bearer token123",
                "X-Custom-Header" to "value"
            )
        )
        
        assertEquals("sse", config.type)
        assertEquals("https://api.example.com/sse", config.url)
        assertEquals(mapOf(
            "Authorization" to "Bearer token123",
            "X-Custom-Header" to "value"
        ), config.headers)
    }
    
    @Test
    fun `McpSSEServerConfig should work with empty headers`() {
        val config = McpSSEServerConfig(
            type = "sse",
            url = "https://api.example.com/sse",
            headers = emptyMap()
        )
        
        assertEquals("sse", config.type)
        assertEquals("https://api.example.com/sse", config.url)
        assertTrue(config.headers.isEmpty())
    }
    
    @Test
    fun `McpHttpServerConfig should store properties correctly`() {
        val config = McpHttpServerConfig(
            type = "http",
            url = "https://api.example.com/http",
            headers = mapOf(
                "Content-Type" to "application/json",
                "API-Key" to "xyz789"
            )
        )
        
        assertEquals("http", config.type)
        assertEquals("https://api.example.com/http", config.url)
        assertEquals(mapOf(
            "Content-Type" to "application/json",
            "API-Key" to "xyz789"
        ), config.headers)
    }
    
    @Test
    fun `McpServerConfig polymorphism should work`() {
        // Test that all configs implement McpServerConfig
        val configs: List<McpServerConfig> = listOf(
            McpStdioServerConfig("stdio", "cmd", emptyList(), emptyMap()),
            McpSSEServerConfig("sse", "url", emptyMap()),
            McpHttpServerConfig("http", "url", emptyMap())
        )
        
        assertEquals(3, configs.size)
        assertTrue(configs[0] is McpStdioServerConfig)
        assertTrue(configs[1] is McpSSEServerConfig)
        assertTrue(configs[2] is McpHttpServerConfig)
    }
    
    @Test
    fun `ClaudeCodeOptions builder should handle null values`() {
        val options = ClaudeCodeOptions.builder()
            .systemPrompt(null)
            .appendSystemPrompt(null)
            .permissionMode(null)
            .resume(null)
            .model(null)
            .cwd(null)
            .permissionPromptToolName(null)
            .build()
        
        assertNull(options.systemPrompt)
        assertNull(options.appendSystemPrompt)
        assertNull(options.permissionMode)
        assertNull(options.resume)
        assertNull(options.model)
        assertNull(options.cwd)
        assertNull(options.permissionPromptToolName)
    }
}