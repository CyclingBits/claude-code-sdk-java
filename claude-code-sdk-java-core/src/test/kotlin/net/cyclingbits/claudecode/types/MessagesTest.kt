
package net.cyclingbits.claudecode.types

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessagesTest {
    
    @Test
    fun `UserMessage should store content correctly`() {
        val message = UserMessage("Hello, Claude!")
        assertEquals("Hello, Claude!", message.content)
        assertTrue(message is Message)
    }
    
    @Test
    fun `AssistantMessage should filter content blocks correctly`() {
        val message = AssistantMessage(
            content = listOf(
                TextBlock("Hello"),
                TextBlock(" World"),
                ToolUseBlock("1", "read_file", buildJsonObject { put("path", "test.txt") }),
                ToolResultBlock("1", JsonPrimitive("File content"))
            )
        )
        
        assertTrue(message is Message)
        assertEquals(2, message.textBlocks.size)
        assertEquals(1, message.toolUseBlocks.size)
        assertEquals(1, message.toolResultBlocks.size)
        assertEquals("Hello World", message.text)
    }
    
    @Test
    fun `AssistantMessage with empty content should work`() {
        val message = AssistantMessage(emptyList())
        
        assertTrue(message is Message)
        assertEquals(0, message.content.size)
        assertEquals(0, message.textBlocks.size)
        assertEquals(0, message.toolUseBlocks.size)
        assertEquals(0, message.toolResultBlocks.size)
        assertEquals("", message.text)
    }
    
    @Test
    fun `SystemMessage should store subtype and data`() {
        val data = buildJsonObject {
            put("info", "System ready")
            put("timestamp", 1234567890)
            putJsonArray("capabilities") {
                add("read")
                add("write")
            }
        }
        
        val message = SystemMessage("initialization", data)
        
        assertTrue(message is Message)
        assertEquals("initialization", message.subtype)
        assertEquals("System ready", message.data["info"]?.jsonPrimitive?.content)
        assertEquals(1234567890, message.data["timestamp"]?.jsonPrimitive?.int)
        assertEquals(2, message.data["capabilities"]?.jsonArray?.size)
    }
    
    @Test
    fun `ResultMessage should extract token counts from usage`() {
        val usage = buildJsonObject {
            put("input_tokens", 100)
            put("output_tokens", 200)
            put("total_tokens", 300)
        }
        
        val message = ResultMessage(
            subtype = "completion",
            durationMs = 1000,
            durationApiMs = 800,
            isError = false,
            numTurns = 1,
            sessionId = "test-session",
            usage = usage
        )
        
        assertTrue(message is Message)
        assertEquals("completion", message.subtype)
        assertEquals(100, message.inputTokens)
        assertEquals(200, message.outputTokens)
        assertEquals(300, message.totalTokens)
    }
    
    @Test
    fun `ResultMessage without usage should return null token counts`() {
        val message = ResultMessage(
            subtype = "error",
            durationMs = 500,
            durationApiMs = 400,
            isError = true,
            numTurns = 0,
            sessionId = "error-session",
            usage = null
        )
        
        assertTrue(message is Message)
        assertEquals("error", message.subtype)
        assertTrue(message.isError)
        assertNull(message.inputTokens)
        assertNull(message.outputTokens)
        assertNull(message.totalTokens)
    }
    
    @Test
    fun `ResultMessage with partial usage data should handle gracefully`() {
        val usage = buildJsonObject {
            put("input_tokens", 50)
            // Missing output_tokens and total_tokens
        }
        
        val message = ResultMessage(
            subtype = "partial",
            durationMs = 200,
            durationApiMs = 150,
            isError = false,
            numTurns = 1,
            sessionId = "partial-session",
            usage = usage
        )
        
        assertEquals(50, message.inputTokens)
        assertNull(message.outputTokens)
        assertNull(message.totalTokens)
    }
}