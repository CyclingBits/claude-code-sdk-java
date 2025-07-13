package net.cyclingbits.claudecode.api

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.cyclingbits.claudecode.types.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtensionsTest {
    
    @Test
    fun `assistantMessages should filter only assistant messages`() = runTest {
        val messages = flowOf(
            UserMessage("Hello"),
            AssistantMessage(listOf(TextBlock("Hi there"))),
            SystemMessage("info", buildJsonObject {}),
            AssistantMessage(listOf(TextBlock("How can I help?")))
        )
        
        val assistantMessages = messages.assistantMessages().toList()
        
        assertEquals(2, assistantMessages.size)
        assertTrue(assistantMessages.all { it is AssistantMessage })
    }
    
    @Test
    fun `userMessages should filter only user messages`() = runTest {
        val messages = flowOf(
            UserMessage("First question"),
            AssistantMessage(listOf(TextBlock("Answer"))),
            UserMessage("Second question"),
            SystemMessage("info", buildJsonObject {})
        )
        
        val userMessages = messages.userMessages().toList()
        
        assertEquals(2, userMessages.size)
        assertTrue(userMessages.all { it is UserMessage })
    }
    
    @Test
    fun `systemMessages should filter only system messages`() = runTest {
        val messages = flowOf(
            UserMessage("Hello"),
            SystemMessage("info", buildJsonObject { put("status", "ready") }),
            AssistantMessage(listOf(TextBlock("Hi"))),
            SystemMessage("warning", buildJsonObject { put("message", "slow") })
        )
        
        val systemMessages = messages.systemMessages().toList()
        
        assertEquals(2, systemMessages.size)
        assertTrue(systemMessages.all { it is SystemMessage })
    }
    
    @Test
    fun `resultMessages should filter only result messages`() = runTest {
        val messages = flowOf(
            UserMessage("Hello"),
            ResultMessage("completion", 100, 80, false, 1, "session-1"),
            AssistantMessage(listOf(TextBlock("Hi"))),
            ResultMessage("error", 50, 40, true, 0, "session-2")
        )
        
        val resultMessages = messages.resultMessages().toList()
        
        assertEquals(2, resultMessages.size)
        assertTrue(resultMessages.all { it is ResultMessage })
    }
    
    @Test
    fun `textContent should extract text from assistant messages`() = runTest {
        val messages = flowOf(
            UserMessage("Hello"),
            AssistantMessage(listOf(TextBlock("First response"))),
            AssistantMessage(listOf(
                TextBlock("Second "),
                TextBlock("response")
            )),
            AssistantMessage(listOf(ToolUseBlock("1", "tool", emptyMap()))),
            AssistantMessage(listOf(TextBlock(""))) // Empty text
        )
        
        val textContent = messages.textContent().toList()
        
        assertEquals(2, textContent.size)
        assertEquals("First response", textContent[0])
        assertEquals("Second response", textContent[1])
    }
    
    @Test
    fun `toolUses should extract tool use blocks`() = runTest {
        val toolUse1 = ToolUseBlock("1", "read_file", emptyMap())
        val toolUse2 = ToolUseBlock("2", "write_file", emptyMap())
        
        val messages = flowOf(
            UserMessage("Hello"),
            AssistantMessage(listOf(
                TextBlock("Let me read that file"),
                toolUse1
            )),
            AssistantMessage(listOf(toolUse2)),
            AssistantMessage(listOf(TextBlock("Done")))
        )
        
        val toolUses = messages.toolUses().toList()
        
        assertEquals(2, toolUses.size)
        assertEquals(toolUse1, toolUses[0])
        assertEquals(toolUse2, toolUses[1])
    }
    
    @Test
    fun `toolResults should extract tool result blocks`() = runTest {
        val toolResult1 = ToolResultBlock("1", null, false)
        val toolResult2 = ToolResultBlock("2", null, true)
        
        val messages = flowOf(
            UserMessage("Hello"),
            AssistantMessage(listOf(
                TextBlock("Here's the result"),
                toolResult1
            )),
            AssistantMessage(listOf(toolResult2)),
            AssistantMessage(listOf(TextBlock("Done")))
        )
        
        val toolResults = messages.toolResults().toList()
        
        assertEquals(2, toolResults.size)
        assertEquals(toolResult1, toolResults[0])
        assertEquals(toolResult2, toolResults[1])
    }
    
    @Test
    fun `successfulMessages should filter out error messages`() = runTest {
        val messages = flowOf(
            UserMessage("Hello"),
            AssistantMessage(listOf(TextBlock("Hi"))),
            ResultMessage("completion", 100, 80, false, 1, "session-1"),
            ResultMessage("error", 50, 40, true, 0, "session-2"),
            SystemMessage("info", buildJsonObject {})
        )
        
        val successfulMessages = messages.successfulMessages().toList()
        
        assertEquals(4, successfulMessages.size)
        // Check that error ResultMessage is not included
        assertFalse(successfulMessages.any { 
            it is ResultMessage && it.isError 
        })
    }
    
    @Test
    fun `errorMessages should filter only error result messages`() = runTest {
        val messages = flowOf(
            UserMessage("Hello"),
            AssistantMessage(listOf(TextBlock("Hi"))),
            ResultMessage("completion", 100, 80, false, 1, "session-1"),
            ResultMessage("error", 50, 40, true, 0, "session-2"),
            SystemMessage("info", buildJsonObject {})
        )
        
        val errorMessages = messages.errorMessages().toList()
        
        assertEquals(1, errorMessages.size)
        val errorMessage = errorMessages[0] as ResultMessage
        assertTrue(errorMessage.isError)
    }
    
    @Test
    fun `assistantMessage helper should create simple text message`() {
        val message = assistantMessage("Hello, world!")
        
        assertEquals(1, message.content.size)
        assertTrue(message.content[0] is TextBlock)
        assertEquals("Hello, world!", message.text)
    }
    
    @Test
    fun `userMessage helper should create user message`() {
        val message = userMessage("Test message")
        
        assertEquals("Test message", message.content)
    }
    
    @Test
    fun `plus operator should combine assistant messages`() {
        val message1 = AssistantMessage(listOf(
            TextBlock("Hello "),
            ToolUseBlock("1", "tool", emptyMap())
        ))
        val message2 = AssistantMessage(listOf(
            TextBlock("world!"),
            ToolResultBlock("1", null, false)
        ))
        
        val combined = message1 + message2
        
        assertEquals(4, combined.content.size)
        assertEquals("Hello world!", combined.text)
        assertEquals(1, combined.toolUseBlocks.size)
        assertEquals(1, combined.toolResultBlocks.size)
    }
}