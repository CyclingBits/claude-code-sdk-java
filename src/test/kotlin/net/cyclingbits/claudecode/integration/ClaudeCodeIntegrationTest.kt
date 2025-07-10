package net.cyclingbits.claudecode.integration

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.cyclingbits.claudecode.api.ClaudeCodeClient
import net.cyclingbits.claudecode.api.assistantMessages
import net.cyclingbits.claudecode.api.textContent
import net.cyclingbits.claudecode.types.AssistantMessage
import net.cyclingbits.claudecode.types.ClaudeCodeOptions
import net.cyclingbits.claudecode.types.ResultMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * Integration tests for Claude Code SDK.
 *
 * These tests make real API calls to Claude Code CLI and should be run manually.
 *
 * To run these tests:
 * 1. Make sure Claude Code CLI is installed: npm install -g @anthropic-ai/claude-code
 * 2. Make sure you're authenticated: claude setup-token
 * 3. Remove or comment out the @Disabled annotation
 * 4. Run the tests normally
 */
@Disabled
class ClaudeCodeIntegrationTest {

    private lateinit var client: ClaudeCodeClient

    @BeforeEach
    fun setup() {
        client = ClaudeCodeClient()
    }

    @Test
    fun `should execute simple query and receive response`() = runBlocking {
        // Given
        val prompt = "What is 2 + 2? Just give me the number."

        // When
        val messages = withTimeout(1.minutes) {
            client.query(prompt).toList()
        }

        // Then
        assertTrue(messages.isNotEmpty(), "Should receive at least one message")

        val assistantMessages = messages.filterIsInstance<AssistantMessage>()
        assertTrue(assistantMessages.isNotEmpty(), "Should receive assistant messages")

        val responseText = assistantMessages.first().text
        assertTrue(responseText.contains("4"), "Response should contain the answer '4'")
    }

    @Test
    fun `should handle query with options`() = runBlocking {
        // Given
        val options = ClaudeCodeOptions(
            systemPrompt = "You are a helpful assistant. Always respond concisely.",
            maxThinkingTokens = 1000,
            timeoutMs = 60_000
        )
        val prompt = "Say 'Hello, World!' and nothing else."

        // When
        val messages = withTimeout(1.minutes) {
            client.query(prompt, options).toList()
        }

        // Then
        val assistantMessages = messages.filterIsInstance<AssistantMessage>()
        assertTrue(assistantMessages.isNotEmpty())

        val responseText = assistantMessages.first().text.lowercase()
        assertTrue(
            responseText.contains("hello") && responseText.contains("world"),
            "Response should contain 'Hello, World!'"
        )
    }

    @Test
    fun `should handle streaming responses`() = runBlocking {
        // Given
        val prompt = "Count from 1 to 3."
        var messageCount = 0

        // When
        withTimeout(1.minutes) {
            client.query(prompt).collect { message ->
                messageCount++
            }
        }

        // Then
        assertTrue(messageCount > 0, "Should receive messages during streaming")
    }

    @Test
    fun `should use extension functions`() = runBlocking {
        // Given
        val prompt = "What is the capital of France? Answer in one word."

        // When
        val textResponses = withTimeout(1.minutes) {
            client.query(prompt)
                .assistantMessages()
                .textContent()
                .toList()
        }

        // Then
        assertTrue(textResponses.isNotEmpty(), "Should extract text content")
        val fullText = textResponses.joinToString("")
        assertTrue(fullText.contains("Paris", ignoreCase = true), "Should contain 'Paris'")
    }

    @Test
    fun `should handle result message with timing info`() = runBlocking {
        // Given
        val prompt = "What is 5 + 5?"

        // When
        val messages = withTimeout(1.minutes) {
            client.query(prompt).toList()
        }

        // Then
        val resultMessage = messages.filterIsInstance<ResultMessage>().firstOrNull()
        assertNotNull(resultMessage, "Should receive a result message")
        assertTrue(resultMessage.durationMs > 0, "Duration should be positive")
    }
}