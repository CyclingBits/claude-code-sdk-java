package net.cyclingbits.claudecode.api

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.cyclingbits.claudecode.types.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtensionsTest {
    
    @Test
    fun `should filter assistant messages correctly`() = runTest {
        val messages = flowOf(
            UserMessage("Hello"),
            AssistantMessage(listOf(TextBlock("Hi"))),
            SystemMessage("info", buildJsonObject { put("data", "test") }),
            AssistantMessage(listOf(TextBlock("How can I help?")))
        )
        
        val assistantMessages = messages.assistantMessages().toList()
        
        assertEquals(2, assistantMessages.size)
        // All items are AssistantMessage by definition of filterIsInstance
        assertEquals(2, assistantMessages.size)
    }
    
    @Test
    fun `should extract text content correctly`() = runTest {
        val messages = flowOf(
            UserMessage("Hello"),
            AssistantMessage(listOf(TextBlock("First response"))),
            AssistantMessage(listOf(
                TextBlock("Second "),
                TextBlock("response")
            )),
            AssistantMessage(listOf(
                ToolUseBlock("1", "tool", buildJsonObject {})
            )),
            AssistantMessage(listOf(TextBlock(""))) // Empty text
        )
        
        val textContent = messages.textContent().toList()
        
        assertEquals(2, textContent.size)
        assertEquals("First response", textContent[0])
        assertEquals("Second response", textContent[1])
    }
    
    @Test
    fun `should extract tool uses correctly`() = runTest {
        val messages = flowOf(
            AssistantMessage(listOf(
                TextBlock("Text"),
                ToolUseBlock("1", "read_file", buildJsonObject { put("path", "test.txt") }),
                ToolUseBlock("2", "write_file", buildJsonObject { put("path", "out.txt") })
            )),
            AssistantMessage(listOf(
                ToolUseBlock("3", "execute", buildJsonObject { put("cmd", "ls") })
            ))
        )
        
        val toolUses = messages.toolUses().toList()
        
        assertEquals(3, toolUses.size)
        assertEquals("read_file", toolUses[0].name)
        assertEquals("write_file", toolUses[1].name)
        assertEquals("execute", toolUses[2].name)
    }
    
    @Test
    fun `should filter successful and error messages`() = runTest {
        val messages = flowOf(
            UserMessage("Test"),
            ResultMessage(
                subtype = "completion",
                durationMs = 100,
                durationApiMs = 80,
                isError = false,
                numTurns = 1,
                sessionId = "123"
            ),
            ResultMessage(
                subtype = "error",
                durationMs = 50,
                durationApiMs = 40,
                isError = true,
                numTurns = 0,
                sessionId = "456",
                result = "Error occurred"
            )
        )
        
        val successful = messages.successfulMessages().toList()
        val errors = messages.errorMessages().toList()
        
        assertEquals(2, successful.size) // UserMessage + successful ResultMessage
        assertEquals(1, errors.size)
        assertTrue((errors[0] as ResultMessage).isError)
    }
    
    @Test
    fun `convenience functions should create correct messages`() {
        val assistant = assistantMessage("Hello from assistant")
        assertEquals(1, assistant.content.size)
        assertEquals("Hello from assistant", assistant.text)
        
        val user = userMessage("Hello from user")
        assertEquals("Hello from user", user.content)
    }
    
    @Test
    fun `should combine assistant messages with plus operator`() {
        val message1 = AssistantMessage(listOf(
            TextBlock("Hello "),
            ToolUseBlock("1", "tool", buildJsonObject {})
        ))
        val message2 = AssistantMessage(listOf(
            TextBlock("World"),
            ToolResultBlock("1", null)
        ))
        
        val combined = message1 + message2
        
        assertEquals(4, combined.content.size)
        assertEquals("Hello World", combined.text)
        assertEquals(1, combined.toolUseBlocks.size)
        assertEquals(1, combined.toolResultBlocks.size)
    }
}