package net.cyclingbits.claudecode.internal.parser

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

class JsonMessageParserTest {
    
    private val parser = JsonMessageParser()
    
    @Test
    fun `should parse user message correctly`() {
        val json = buildJsonObject {
            put("type", "user")
            putJsonObject("message") {
                put("content", "Hello, Claude!")
            }
        }
        
        val message = parser.parseMessage(json)
        
        assertNotNull(message)
        assertTrue(message is net.cyclingbits.claudecode.types.UserMessage)
        assertEquals("Hello, Claude!", message.content)
    }
    
    @Test
    fun `should parse assistant message with text block`() {
        val json = buildJsonObject {
            put("type", "assistant")
            putJsonObject("message") {
                putJsonArray("content") {
                    addJsonObject {
                        put("type", "text")
                        put("text", "Hello! How can I help you?")
                    }
                }
            }
        }
        
        val message = parser.parseMessage(json)
        
        assertNotNull(message)
        assertTrue(message is net.cyclingbits.claudecode.types.AssistantMessage)
        assertEquals(1, message.content.size)
        
        val textBlock = message.content[0]
        assertTrue(textBlock is net.cyclingbits.claudecode.types.TextBlock)
        assertEquals("Hello! How can I help you?", textBlock.text)
    }
    
    @Test
    fun `should parse assistant message with tool use`() {
        val json = buildJsonObject {
            put("type", "assistant")
            putJsonObject("message") {
                putJsonArray("content") {
                    addJsonObject {
                        put("type", "tool_use")
                        put("id", "tool_123")
                        put("name", "read_file")
                        putJsonObject("input") {
                            put("path", "/tmp/test.txt")
                        }
                    }
                }
            }
        }
        
        val message = parser.parseMessage(json)
        
        assertNotNull(message)
        assertTrue(message is net.cyclingbits.claudecode.types.AssistantMessage)
        assertEquals(1, message.content.size)
        
        val toolUse = message.content[0]
        assertTrue(toolUse is net.cyclingbits.claudecode.types.ToolUseBlock)
        assertEquals("tool_123", toolUse.id)
        assertEquals("read_file", toolUse.name)
        assertEquals("/tmp/test.txt", toolUse.input["path"]?.jsonPrimitive?.content)
    }
    
    @Test
    fun `should parse assistant message with tool result`() {
        val json = buildJsonObject {
            put("type", "assistant")
            putJsonObject("message") {
                putJsonArray("content") {
                    addJsonObject {
                        put("type", "tool_result")
                        put("tool_use_id", "tool_123")
                        put("content", "File content here")
                        put("is_error", false)
                    }
                }
            }
        }
        
        val message = parser.parseMessage(json)
        
        assertNotNull(message)
        assertTrue(message is net.cyclingbits.claudecode.types.AssistantMessage)
        assertEquals(1, message.content.size)
        
        val toolResult = message.content[0]
        assertTrue(toolResult is net.cyclingbits.claudecode.types.ToolResultBlock)
        assertEquals("tool_123", toolResult.toolUseId)
        assertEquals("File content here", toolResult.content?.jsonPrimitive?.content)
        assertEquals(false, toolResult.isError)
    }
    
    @Test
    fun `should parse system message`() {
        val json = buildJsonObject {
            put("type", "system")
            put("subtype", "info")
            put("message", "System information")
            put("timestamp", 1234567890)
        }
        
        val message = parser.parseMessage(json)
        
        assertNotNull(message)
        assertTrue(message is net.cyclingbits.claudecode.types.SystemMessage)
        assertEquals("info", message.subtype)
        assertEquals("System information", message.data["message"]?.jsonPrimitive?.content)
        assertEquals(1234567890, message.data["timestamp"]?.jsonPrimitive?.int)
        // Should not include 'type' in data
        assertNull(message.data["type"])
    }
    
    @Test
    fun `should parse result message`() {
        val json = buildJsonObject {
            put("type", "result")
            put("subtype", "completion")
            put("duration_ms", 1500)
            put("duration_api_ms", 1200)
            put("is_error", false)
            put("num_turns", 2)
            put("session_id", "session_123")
            put("total_cost_usd", 0.05)
            putJsonObject("usage") {
                put("input_tokens", 100)
                put("output_tokens", 200)
                put("total_tokens", 300)
            }
            put("result", "Task completed successfully")
        }
        
        val message = parser.parseMessage(json)
        
        assertNotNull(message)
        assertTrue(message is net.cyclingbits.claudecode.types.ResultMessage)
        assertEquals("completion", message.subtype)
        assertEquals(1500, message.durationMs)
        assertEquals(1200, message.durationApiMs)
        assertEquals(false, message.isError)
        assertEquals(2, message.numTurns)
        assertEquals("session_123", message.sessionId)
        assertEquals(0.05, message.totalCostUsd)
        assertEquals("Task completed successfully", message.result)
        
        // Check usage
        assertNotNull(message.usage)
        assertEquals(100, message.inputTokens)
        assertEquals(200, message.outputTokens)
        assertEquals(300, message.totalTokens)
    }
    
    @Test
    fun `should return null for unknown message type`() {
        val json = buildJsonObject {
            put("type", "unknown")
            put("data", "some data")
        }
        
        val message = parser.parseMessage(json)
        
        assertNull(message)
    }
    
    @Test
    fun `should handle missing required fields gracefully`() {
        val json = buildJsonObject {
            put("type", "user")
            // Missing message field
        }
        
        val message = parser.parseMessage(json)
        
        assertNull(message)
    }
    
    @Test
    fun `should parse assistant message with multiple content blocks`() {
        val json = buildJsonObject {
            put("type", "assistant")
            putJsonObject("message") {
                putJsonArray("content") {
                    addJsonObject {
                        put("type", "text")
                        put("text", "Let me help you with that.")
                    }
                    addJsonObject {
                        put("type", "tool_use")
                        put("id", "tool_1")
                        put("name", "read_file")
                        putJsonObject("input") {
                            put("path", "test.txt")
                        }
                    }
                    addJsonObject {
                        put("type", "tool_result")
                        put("tool_use_id", "tool_1")
                        put("content", "File contents")
                    }
                    addJsonObject {
                        put("type", "text")
                        put("text", "Here's what I found.")
                    }
                }
            }
        }
        
        val message = parser.parseMessage(json)
        
        assertNotNull(message)
        assertTrue(message is net.cyclingbits.claudecode.types.AssistantMessage)
        assertEquals(4, message.content.size)
        assertEquals(2, message.textBlocks.size)
        assertEquals(1, message.toolUseBlocks.size)
        assertEquals(1, message.toolResultBlocks.size)
        assertEquals("Let me help you with that.Here's what I found.", message.text)
    }
}