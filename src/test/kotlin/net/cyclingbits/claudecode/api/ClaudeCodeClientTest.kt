package net.cyclingbits.claudecode.api

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.cyclingbits.claudecode.types.ClaudeCodeOptions
import org.junit.jupiter.api.Test
import kotlin.test.*

class ClaudeCodeClientTest {
    
    @Test
    fun `DSL builder should set properties correctly`() = runTest {
        // This test verifies the DSL syntax compiles and works correctly
        val client = ClaudeCodeClient()
        
        // We can't test actual execution without mocking internal components
        // but we can verify the DSL compiles
        val queryBlock: QueryBuilder.() -> Unit = {
            prompt = "Test prompt"
            options {
                allowedTools = listOf("read", "write")
                systemPrompt = "Test system prompt"
                maxThinkingTokens = 10000
                model = "claude-3-5-sonnet-20241022"
                cwd = kotlin.io.path.Path("/tmp")
                maxTurns = 3
                continueConversation = false
            }
        }
        
        val builder = QueryBuilder().apply(queryBlock)
        assertEquals("Test prompt", builder.prompt)
        
        val options = builder.buildOptions()
        assertEquals(listOf("read", "write"), options.allowedTools)
        assertEquals("Test system prompt", options.systemPrompt)
        assertEquals(10000, options.maxThinkingTokens)
        assertEquals("claude-3-5-sonnet-20241022", options.model)
        assertEquals(3, options.maxTurns)
        assertFalse(options.continueConversation)
    }
    
    @Test
    fun `options builder should handle all properties`() {
        val initialOptions = ClaudeCodeOptions()
        val builder = OptionsBuilder(initialOptions)
        
        builder.allowedTools = listOf("tool1", "tool2")
        builder.disallowedTools = listOf("tool3")
        builder.systemPrompt = "System"
        builder.appendSystemPrompt = "Append"
        builder.maxThinkingTokens = 5000
        builder.model = "model-name"
        builder.cwd = kotlin.io.path.Path("/work")
        builder.maxTurns = 10
        builder.continueConversation = true
        builder.resume = "token123"
        
        val options = builder.build()
        
        assertEquals(listOf("tool1", "tool2"), options.allowedTools)
        assertEquals(listOf("tool3"), options.disallowedTools)
        assertEquals("System", options.systemPrompt)
        assertEquals("Append", options.appendSystemPrompt)
        assertEquals(5000, options.maxThinkingTokens)
        assertEquals("model-name", options.model)
        assertNotNull(options.cwd)
        assertEquals(10, options.maxTurns)
        assertTrue(options.continueConversation)
        assertEquals("token123", options.resume)
    }
    
    @Test
    fun `Java API methods should be accessible`() {
        val client = ClaudeCodeClient()
        
        // Test that Java-friendly methods exist and are callable
        assertNotNull(client::queryAsync)
        assertNotNull(client::queryWithCallback)
        
        // Test default parameters work
        val future = client.queryAsync("Test")
        assertNotNull(future)
        
        // Test with options
        val options = ClaudeCodeOptions.builder()
            .allowedTools(listOf("read"))
            .build()
        val futureWithOptions = client.queryAsync("Test", options)
        assertNotNull(futureWithOptions)
    }
}