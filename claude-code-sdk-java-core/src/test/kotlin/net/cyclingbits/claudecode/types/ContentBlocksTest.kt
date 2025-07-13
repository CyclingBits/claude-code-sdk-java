package net.cyclingbits.claudecode.types

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContentBlocksTest {
    
    @Test
    fun `TextBlock should have correct properties`() {
        val block = TextBlock("Hello, world!")
        
        assertEquals("Hello, world!", block.text)
        assertTrue(block is ContentBlock)
    }
    
    @Test
    fun `ToolUseBlock should have correct properties`() {
        val input = mapOf(
            "path" to JsonPrimitive("/tmp/test.txt"),
            "recursive" to JsonPrimitive(true)
        )
        
        val block = ToolUseBlock(
            id = "tool_123",
            name = "read_file",
            input = input
        )
        
        assertEquals("tool_123", block.id)
        assertEquals("read_file", block.name)
        assertEquals("/tmp/test.txt", block.input["path"]?.jsonPrimitive?.content)
        assertTrue(block.input["recursive"]?.jsonPrimitive?.boolean ?: false)
        assertTrue(block is ContentBlock)
    }
    
    @Test
    fun `ToolUseBlock with empty input should work`() {
        val block = ToolUseBlock(
            id = "tool_456",
            name = "list_files",
            input = emptyMap()
        )
        
        assertEquals("tool_456", block.id)
        assertEquals("list_files", block.name)
        assertEquals(0, block.input.size)
    }
    
    @Test
    fun `ToolResultBlock should handle string content`() {
        val block = ToolResultBlock(
            toolUseId = "tool_123",
            content = JsonPrimitive("File content here"),
            isError = false
        )
        
        assertEquals("tool_123", block.toolUseId)
        assertEquals("File content here", block.content?.jsonPrimitive?.content)
        assertFalse(block.isError ?: true)
        assertTrue(block is ContentBlock)
    }
    
    @Test
    fun `ToolResultBlock should handle object content`() {
        val content = buildJsonObject {
            put("status", "success")
            put("data", "Result data")
        }
        
        val block = ToolResultBlock(
            toolUseId = "tool_789",
            content = content,
            isError = false
        )
        
        assertEquals("tool_789", block.toolUseId)
        assertEquals("success", block.content?.jsonObject?.get("status")?.jsonPrimitive?.content)
        assertFalse(block.isError ?: true)
    }
    
    @Test
    fun `ToolResultBlock should handle error state`() {
        val block = ToolResultBlock(
            toolUseId = "tool_error",
            content = JsonPrimitive("Error occurred"),
            isError = true
        )
        
        assertEquals("tool_error", block.toolUseId)
        assertTrue(block.isError ?: false)
    }
    
    @Test
    fun `ToolResultBlock should handle null content`() {
        val block = ToolResultBlock(
            toolUseId = "tool_null",
            content = null,
            isError = null
        )
        
        assertEquals("tool_null", block.toolUseId)
        assertNull(block.content)
        assertNull(block.isError)
    }
}