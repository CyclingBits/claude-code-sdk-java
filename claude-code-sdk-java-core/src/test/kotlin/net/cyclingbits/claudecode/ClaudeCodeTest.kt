package net.cyclingbits.claudecode

import net.cyclingbits.claudecode.api.ClaudeCodeClient
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ClaudeCodeTest {
    
    @Test
    fun `claudeCodeClient function should create new instance`() {
        // This uses the constructor without CLI path, which skips verification
        val client = claudeCodeClient()
        assertNotNull(client)
    }
    
    @Test
    fun `claudeCodeClient function should create different instances`() {
        val client1 = claudeCodeClient()
        val client2 = claudeCodeClient()
        
        assertNotSame(client1, client2, "Should create different instances")
    }
    
    @Test
    fun `ClaudeCode is singleton object`() {
        // Verify ClaudeCode is a singleton
        val instance1 = ClaudeCode
        val instance2 = ClaudeCode
        
        assertSame(instance1, instance2, "ClaudeCode should be a singleton")
    }
}