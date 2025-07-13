package net.cyclingbits.claudecode.types

import net.cyclingbits.claudecode.exceptions.ClaudeSDKException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OutcomeTest {
    
    @Test
    fun `extension functions should work correctly`() {
        val successOutcome: Outcome<String> = Outcome.Success(
            value = "test",
            messages = emptyList(),
            durationMs = 100
        )
        
        // Test map
        val mapped = successOutcome.map { it.uppercase() }
        assertTrue(mapped is Outcome.Success)
        assertEquals("TEST", (mapped as Outcome.Success).value)
        
        // Test getOrNull
        assertEquals("test", successOutcome.getOrNull())
        assertNull(Outcome.Error(ClaudeSDKException("error")).getOrNull())
        
        // Test getOrThrow
        assertEquals("test", successOutcome.getOrThrow())
        
        // Test onSuccess
        var successCalled = false
        successOutcome.onSuccess { successCalled = true }
        assertTrue(successCalled)
        
        // Test onError
        var errorCalled = false
        Outcome.Error(ClaudeSDKException("error")).onError { errorCalled = true }
        assertTrue(errorCalled)
        
        // Test onTimeout
        var timeoutCalled = false
        Outcome.Timeout(1000).onTimeout { timeoutCalled = true }
        assertTrue(timeoutCalled)
    }
    
    @Test
    fun `Success should contain value and have correct properties`() {
        val messages = listOf(
            UserMessage("Test"),
            AssistantMessage(listOf(TextBlock("Success!")))
        )
        val outcome: Outcome<List<Message>> = Outcome.Success(
            value = messages,
            messages = messages,
            durationMs = 1000
        )
        
        assertTrue(outcome is Outcome.Success)
        assertFalse(outcome is Outcome.Error)
        assertFalse(outcome is Outcome.Timeout)
        assertEquals(messages, outcome.value)
        assertEquals(1000, outcome.durationMs)
    }
    
    @Test
    fun `Error should contain exception and have correct properties`() {
        val exception = ClaudeSDKException("Something went wrong")
        val partialMessages = listOf(UserMessage("Test"))
        val outcome: Outcome<Nothing> = Outcome.Error(
            exception = exception,
            partialMessages = partialMessages,
            durationMs = 500
        )
        
        assertFalse(outcome is Outcome.Success)
        assertTrue(outcome is Outcome.Error)
        assertFalse(outcome is Outcome.Timeout)
        assertEquals(exception, outcome.exception)
        assertEquals(partialMessages, outcome.partialMessages)
        assertEquals(500, outcome.durationMs)
    }
    
    @Test
    fun `Timeout should have correct properties`() {
        val partialMessages = listOf(UserMessage("Test"))
        val outcome: Outcome<Nothing> = Outcome.Timeout(
            durationMs = 30000,
            partialMessages = partialMessages
        )
        
        assertFalse(outcome is Outcome.Success)
        assertFalse(outcome is Outcome.Error)
        assertTrue(outcome is Outcome.Timeout)
        assertEquals(30000, outcome.durationMs)
        assertEquals(partialMessages, outcome.partialMessages)
    }
    
    @Test
    fun `when block should work correctly`() {
        val successOutcome: Outcome<String> = Outcome.Success(
            value = "test",
            messages = emptyList(),
            durationMs = 100
        )
        val errorOutcome: Outcome<String> = Outcome.Error(
            exception = ClaudeSDKException("error")
        )
        val timeoutOutcome: Outcome<String> = Outcome.Timeout(
            durationMs = 5000
        )
        
        var successCalled = false
        var errorCalled = false
        var timeoutCalled = false
        
        when (successOutcome) {
            is Outcome.Success -> successCalled = true
            is Outcome.Error -> errorCalled = true
            is Outcome.Timeout -> timeoutCalled = true
        }
        
        assertTrue(successCalled)
        assertFalse(errorCalled)
        assertFalse(timeoutCalled)
        
        successCalled = false
        
        when (errorOutcome) {
            is Outcome.Success -> successCalled = true
            is Outcome.Error -> errorCalled = true
            is Outcome.Timeout -> timeoutCalled = true
        }
        
        assertFalse(successCalled)
        assertTrue(errorCalled)
        assertFalse(timeoutCalled)
        
        errorCalled = false
        
        when (timeoutOutcome) {
            is Outcome.Success -> successCalled = true
            is Outcome.Error -> errorCalled = true
            is Outcome.Timeout -> timeoutCalled = true
        }
        
        assertFalse(successCalled)
        assertFalse(errorCalled)
        assertTrue(timeoutCalled)
    }
}