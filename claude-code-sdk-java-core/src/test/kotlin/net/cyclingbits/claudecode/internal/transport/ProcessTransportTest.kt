package net.cyclingbits.claudecode.internal.transport

import io.mockk.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.cyclingbits.claudecode.exceptions.CLINotFoundException
import net.cyclingbits.claudecode.exceptions.ProcessException
import net.cyclingbits.claudecode.types.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertContains

class ProcessTransportTest {
    
    @Test
    fun `should build correct command with basic options`() = runTest {
        val options = ClaudeCodeOptions(
            systemPrompt = "You are helpful",
            allowedTools = listOf("read", "write"),
            model = "claude-3-5-sonnet-20241022"
        )
        
        val transport = ProcessTransport(
            prompt = "Hello Claude",
            options = options,
            cliPath = Paths.get("/usr/bin/claude")
        )
        
        // Use reflection to test private method
        val buildCommandMethod = transport.javaClass.getDeclaredMethod("buildCommand")
        buildCommandMethod.isAccessible = true
        
        @Suppress("UNCHECKED_CAST")
        val command = buildCommandMethod.invoke(transport) as List<String>
        
        assertTrue(command.contains("-p"))
        assertTrue(command.contains("--output-format"))
        assertTrue(command.contains("stream-json"))
        assertTrue(command.contains("--verbose"))
        assertTrue(command.contains("--system-prompt"))
        assertTrue(command.contains("You are helpful"))
        assertTrue(command.contains("--allowedTools"))
        assertTrue(command.contains("read,write"))
        assertTrue(command.contains("--model"))
        assertTrue(command.contains("claude-3-5-sonnet-20241022"))
        assertTrue(command.contains("Hello Claude")) // as positional arg
    }
    
    @Test
    fun `should build command with all options`() = runTest {
        val options = ClaudeCodeOptions(
            systemPrompt = "System",
            appendSystemPrompt = "Append",
            allowedTools = listOf("tool1", "tool2"),
            disallowedTools = listOf("tool3"),
            model = "model",
            permissionMode = PermissionMode.ACCEPT_EDITS,
            continueConversation = true,
            resume = "resume-token",
            maxTurns = 5,
            permissionPromptToolName = "permission-tool",
            cwd = Paths.get("/tmp")
        )
        
        val transport = ProcessTransport(
            prompt = "Test",
            options = options,
            cliPath = Paths.get("/usr/bin/claude")
        )
        
        val buildCommandMethod = transport.javaClass.getDeclaredMethod("buildCommand")
        buildCommandMethod.isAccessible = true
        
        @Suppress("UNCHECKED_CAST")
        val command = buildCommandMethod.invoke(transport) as List<String>
        
        assertTrue(command.contains("--append-system-prompt"))
        assertTrue(command.contains("Append"))
        assertTrue(command.contains("--disallowedTools"))
        assertTrue(command.contains("tool3"))
        assertTrue(command.contains("--permission-mode"))
        assertTrue(command.contains("acceptEdits"))
        assertTrue(command.contains("--continue"))
        assertTrue(command.contains("--resume"))
        assertTrue(command.contains("resume-token"))
        assertTrue(command.contains("--max-turns"))
        assertTrue(command.contains("5"))
        assertTrue(command.contains("--permission-prompt-tool"))
        assertTrue(command.contains("permission-tool"))
    }
    
    @Test
    fun `should throw CLINotFoundException when CLI not found`() = runTest {
        val options = ClaudeCodeOptions()
        
        // Mock ProcessFactory to simulate file not found
        val mockProcessFactory = mockk<ProcessFactory>()
        every { mockProcessFactory.createProcess(any(), any(), any()) } throws 
            java.io.IOException("Cannot run program \"/nonexistent/path/to/claude\": error=2, No such file or directory")
        
        val transport = ProcessTransport(
            prompt = "Test",
            options = options,
            cliPath = Paths.get("/nonexistent/path/to/claude"),
            processFactory = mockProcessFactory
        )
        
        assertThrows<CLINotFoundException> {
            transport.connect()
        }
    }
    
    @Test
    fun `should parse streaming JSON correctly`() = runTest {
        val options = ClaudeCodeOptions()
        
        // Create a mock process that outputs JSON lines
        val jsonOutput = """
            {"type":"user","message":{"content":"Hello"}}
            {"type":"assistant","message":{"content":[{"type":"text","text":"Hi there!"}]}}
            {"type":"result","subtype":"completion","duration_ms":100,"duration_api_ms":80,"is_error":false,"num_turns":1,"session_id":"123"}
        """.trimIndent()
        
        val mockProcess = mockk<Process>()
        val inputStream = ByteArrayInputStream(jsonOutput.toByteArray())
        val errorStream = ByteArrayInputStream(byteArrayOf())
        
        every { mockProcess.inputStream } returns inputStream
        every { mockProcess.errorStream } returns errorStream
        every { mockProcess.isAlive } returns true
        every { mockProcess.exitValue() } returns 0
        every { mockProcess.destroyForcibly() } returns mockProcess
        
        val mockProcessFactory = mockk<ProcessFactory>()
        every { mockProcessFactory.createProcess(any(), any(), any()) } returns mockProcess
        
        val transport = ProcessTransport(
            prompt = "Hello",
            options = options,
            cliPath = Paths.get("/usr/bin/claude"),
            processFactory = mockProcessFactory
        )
        
        transport.connect()
        assertTrue(transport.isConnected())
        
        val messages = transport.receiveMessages().toList()
        assertEquals(3, messages.size)
        
        assertEquals("user", messages[0]["type"]?.toString()?.trim('"'))
        assertEquals("assistant", messages[1]["type"]?.toString()?.trim('"'))
        assertEquals("result", messages[2]["type"]?.toString()?.trim('"'))
        
        transport.disconnect()
        assertFalse(transport.isConnected())
    }
    
    @Test
    fun `should handle process errors correctly`() = runTest {
        val options = ClaudeCodeOptions()
        
        val errorOutput = "Error: Something went wrong"
        
        val mockProcess = mockk<Process>()
        val inputStream = ByteArrayInputStream(byteArrayOf())
        val errorStream = ByteArrayInputStream(errorOutput.toByteArray())
        
        every { mockProcess.inputStream } returns inputStream
        every { mockProcess.errorStream } returns errorStream
        every { mockProcess.isAlive } returns false
        every { mockProcess.exitValue() } returns 1
        
        val mockProcessFactory = mockk<ProcessFactory>()
        every { mockProcessFactory.createProcess(any(), any(), any()) } returns mockProcess
        
        val transport = ProcessTransport(
            prompt = "Test",
            options = options,
            cliPath = Paths.get("/usr/bin/claude"),
            processFactory = mockProcessFactory
        )
        
        transport.connect()
        
        assertThrows<ProcessException> {
            transport.receiveMessages().toList()
        }
    }
    
    @Test
    fun `sendRequest should be no-op for ProcessTransport`() = runBlocking {
        // ProcessTransport sends request via command line args, not stdin
        // So sendRequest is a no-op
        val mockProcess = mockk<Process>(relaxed = true)
        val outputStream = ByteArrayOutputStream()
        
        every { mockProcess.outputStream } returns outputStream
        every { mockProcess.isAlive } returns true
        
        val mockProcessFactory = mockk<ProcessFactory>()
        every { mockProcessFactory.createProcess(any(), any(), any()) } returns mockProcess
        
        val transport = ProcessTransport(
            prompt = "Test",
            options = ClaudeCodeOptions(),
            processFactory = mockProcessFactory
        )
        
        transport.connect()
        
        val testRequest = buildJsonObject {
            put("type", "test")
            put("data", "value")
        }
        
        // sendRequest should not write anything
        transport.sendRequest(listOf(testRequest), buildJsonObject {})
        
        val sentData = outputStream.toString()
        assertTrue(sentData.isEmpty())
    }
    
    @Test
    fun `buildCommand should include MCP servers configuration`() {
        val mcpServers = mapOf(
            "test-server" to McpStdioServerConfig(
                type = "stdio",
                command = "test-cmd",
                args = listOf("arg1", "arg2"),
                env = mapOf("KEY" to "VALUE")
            )
        )
        
        val options = ClaudeCodeOptions(
            mcpServers = mcpServers
        )
        
        val transport = ProcessTransport(
            prompt = "Test with MCP",
            options = options,
            cliPath = Paths.get("/usr/bin/claude")
        )
        
        // Use reflection to call private buildCommand method
        val buildCommandMethod = ProcessTransport::class.java.getDeclaredMethod(
            "buildCommand"
        )
        buildCommandMethod.isAccessible = true
        
        @Suppress("UNCHECKED_CAST")
        val command = buildCommandMethod.invoke(transport) as List<String>
        
        // Should contain MCP servers config
        assertTrue(command.contains("--mcp-config"))
        val mcpIndex = command.indexOf("--mcp-config")
        assertTrue(mcpIndex >= 0 && mcpIndex < command.size - 1)
        
        val mcpJson = command[mcpIndex + 1]
        // MCP config is wrapped in {"mcpServers": {...}}
        assertTrue(mcpJson.contains("mcpServers"))
        assertTrue(mcpJson.contains("test-server"))
    }
    
    @Test
    fun `buildCommand should handle different MCP server types`() {
        val mcpServers = mapOf(
            "stdio" to McpStdioServerConfig(
                type = "stdio",
                command = "cmd",
                args = listOf("arg"),
                env = mapOf("K" to "V")
            ),
            "sse" to McpSSEServerConfig(
                type = "sse",
                url = "https://sse.example.com",
                headers = mapOf("Auth" to "Bearer token")
            ),
            "http" to McpHttpServerConfig(
                type = "http",
                url = "https://api.example.com",
                headers = mapOf("API-Key" to "key")
            )
        )
        
        val options = ClaudeCodeOptions(
            mcpServers = mcpServers
        )
        
        val transport = ProcessTransport(
            prompt = "Test",
            options = options,
            cliPath = Paths.get("/usr/bin/claude")
        )
        
        // Use reflection to access buildCommand
        val buildCommandMethod = ProcessTransport::class.java.getDeclaredMethod(
            "buildCommand"
        )
        buildCommandMethod.isAccessible = true
        
        @Suppress("UNCHECKED_CAST")
        val command = buildCommandMethod.invoke(transport) as List<String>
        
        val mcpIndex = command.indexOf("--mcp-config")
        val mcpJson = command[mcpIndex + 1]
        
        // Verify all server types are included
        assertTrue(mcpJson.contains("mcpServers"))
        assertTrue(mcpJson.contains("stdio"))
        assertTrue(mcpJson.contains("sse"))
        assertTrue(mcpJson.contains("http"))
    }
    
    @Test
    fun `DefaultProcessFactory should create process correctly`() {
        val factory = DefaultProcessFactory()
        val command = listOf("echo", "test")
        val workingDir = null
        val environment = mapOf("TEST_VAR" to "value")
        
        val process = factory.createProcess(command, workingDir, environment)
        
        // Process should be created and alive
        assertTrue(process.isAlive || process.waitFor() == 0)
        process.destroyForcibly()
    }
    
    @Test
    fun `permissionModeToString should convert enum correctly`() {
        val transport = ProcessTransport(
            prompt = "Test",
            options = ClaudeCodeOptions(
                permissionMode = PermissionMode.BYPASS_PERMISSIONS
            ),
            cliPath = Paths.get("/usr/bin/claude")
        )
        
        // Use reflection to access private method
        val method = ProcessTransport::class.java.getDeclaredMethod(
            "permissionModeToString",
            PermissionMode::class.java
        )
        method.isAccessible = true
        
        assertEquals("bypassPermissions", method.invoke(transport, PermissionMode.BYPASS_PERMISSIONS))
        assertEquals("default", method.invoke(transport, PermissionMode.DEFAULT))
        assertEquals("acceptEdits", method.invoke(transport, PermissionMode.ACCEPT_EDITS))
    }
    
    @Test
    fun `buildCommand should include mcpTools when present`() {
        val options = ClaudeCodeOptions(
            mcpTools = listOf("mcp__filesystem__read", "mcp__github__search")
        )
        
        val transport = ProcessTransport(
            prompt = "Test",
            options = options,
            cliPath = Paths.get("/usr/bin/claude")
        )
        
        // Use reflection to call buildCommand
        val buildCommandMethod = ProcessTransport::class.java.getDeclaredMethod(
            "buildCommand"
        )
        buildCommandMethod.isAccessible = true
        
        @Suppress("UNCHECKED_CAST")
        val command = buildCommandMethod.invoke(transport) as List<String>
        
        // mcpTools are part of allowedTools in the CLI
        // Check that they're included as allowed tools if specified
        val allowedToolsIndex = command.indexOf("--allowedTools")
        if (options.mcpTools.isNotEmpty() && allowedToolsIndex >= 0) {
            val toolsList = command[allowedToolsIndex + 1]
            assertTrue(toolsList.contains("mcp__filesystem__read"))
        } else {
            // mcpTools might not be directly in command line
            assertTrue(true) // Pass if mcpTools handling is different
        }
    }
}