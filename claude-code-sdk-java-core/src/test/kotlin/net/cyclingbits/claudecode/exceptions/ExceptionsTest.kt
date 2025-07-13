package net.cyclingbits.claudecode.exceptions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExceptionsTest {
    
    @Test
    fun `CLIConnectionException should extend ClaudeSDKException`() {
        val exception = CLIConnectionException("Connection failed")
        assertEquals("Connection failed", exception.message)
        assertTrue(exception is ClaudeSDKException)
    }
    
    @Test
    fun `CLIJSONDecodeException should have line and cause`() {
        val cause = RuntimeException("Parse error")
        val exception = CLIJSONDecodeException(
            line = "{invalid json}",
            cause = cause
        )
        assertEquals("{invalid json}", exception.line)
        assertEquals(cause, exception.cause)
        assertTrue(exception.message?.contains("Failed to decode JSON") == true)
        assertTrue(exception is ClaudeSDKException)
    }
    
    @Test
    fun `CLITimeoutException should extend ClaudeSDKException`() {
        val exception = CLITimeoutException("Request timed out")
        assertEquals("Request timed out", exception.message)
        assertTrue(exception is ClaudeSDKException)
    }
    
    @Test
    fun `ClaudeSDKException should have message and optional cause`() {
        val exception = ClaudeSDKException("Test error")
        assertEquals("Test error", exception.message)
        assertNull(exception.cause)
        
        val cause = RuntimeException("Root cause")
        val exceptionWithCause = ClaudeSDKException("Wrapped error", cause)
        assertEquals("Wrapped error", exceptionWithCause.message)
        assertEquals(cause, exceptionWithCause.cause)
    }
    
    @Test
    fun `CLINotFoundException should extend CLIConnectionException`() {
        val exception = CLINotFoundException()
        assertTrue(exception.message?.contains("Claude Code CLI not found") == true)
        assertTrue(exception is CLIConnectionException)
        assertTrue(exception is ClaudeSDKException)
    }
    
    @Test
    fun `ProcessException should have exit code and stderr`() {
        val exception = ProcessException(exitCode = 1, stderr = "Error output")
        assertEquals(1, exception.exitCode)
        assertEquals("Error output", exception.stderr)
        assertTrue(exception.message?.contains("exited with code 1") == true)
        assertTrue(exception is ClaudeSDKException)
    }
    
    @Test
    fun `TimeoutException should have timeout duration`() {
        val exception = TimeoutException(timeoutMs = 5000)
        assertEquals(5000, exception.timeoutMs)
        assertTrue(exception.message?.contains("timed out after 5000ms") == true)
        assertTrue(exception is ClaudeSDKException)
    }
    
    @Test
    fun `ConfigurationException should extend ClaudeSDKException`() {
        val exception = ConfigurationException("Invalid model specified")
        assertEquals("Invalid model specified", exception.message)
        assertTrue(exception is ClaudeSDKException)
    }
    
    @Test
    fun `exceptions should be throwable`() {
        assertThrows<ClaudeSDKException> {
            throw ClaudeSDKException("Test throw")
        }
        
        assertThrows<CLINotFoundException> {
            throw CLINotFoundException("CLI not found")
        }
        
        assertThrows<ProcessException> {
            throw ProcessException(exitCode = 1, stderr = "Process error")
        }
        
        assertThrows<TimeoutException> {
            throw TimeoutException(timeoutMs = 5000)
        }
        
        assertThrows<ConfigurationException> {
            throw ConfigurationException("Bad config")
        }
    }
    
    @Test
    fun `exceptions should support stack traces`() {
        try {
            throw ProcessException(exitCode = 1, stderr = "Test stack trace")
        } catch (e: ProcessException) {
            assertNotNull(e.stackTrace)
            assertTrue(e.stackTrace.isNotEmpty())
        }
    }
}