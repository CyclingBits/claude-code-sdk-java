
package net.cyclingbits.claudecode.types

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessagesTest {
    
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
        
        assertEquals(2, message.textBlocks.size)
        assertEquals(1, message.toolUseBlocks.size)
        assertEquals(1, message.toolResultBlocks.size)
        assertEquals("Hello World", message.text)
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
        
        assertEquals(100, message.inputTokens)
        assertEquals(200, message.outputTokens)
        assertEquals(300, message.totalTokens)
    }
}