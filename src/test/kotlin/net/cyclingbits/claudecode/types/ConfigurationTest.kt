package net.cyclingbits.claudecode.types

import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
}