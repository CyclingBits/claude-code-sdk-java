package net.cyclingbits.claudecode

import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class ClaudeCodeTest {
    
    @Test
    fun `ClaudeCode is singleton object`() {
        // Verify ClaudeCode is a singleton
        val instance1 = ClaudeCode
        val instance2 = ClaudeCode
        
        assertSame(instance1, instance2, "ClaudeCode should be a singleton")
    }
    
    // Note: We cannot test createClient() methods without mocking
    // because they verify CLI availability, which is not available in CI.
    // These are tested indirectly through ClaudeCodeClientTest
    // which uses mocked internal client.
}