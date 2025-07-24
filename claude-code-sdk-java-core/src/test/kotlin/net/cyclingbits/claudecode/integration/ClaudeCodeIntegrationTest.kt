package net.cyclingbits.claudecode.integration

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import net.cyclingbits.claudecode.api.ClaudeCodeClient
import net.cyclingbits.claudecode.types.AssistantMessage
import net.cyclingbits.claudecode.types.SystemMessage
import net.cyclingbits.claudecode.types.ResultMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * Integration test for Claude Code SDK.
 *
 * This test makes real API calls to Claude Code CLI.
 *
 * Requirements:
 * 1. Claude Code CLI must be installed: npm install -g @anthropic-ai/claude-code
 * 2. You must be authenticated
 */
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
        println("Prompt: $prompt")

        try {
            // When
            val messages = withTimeout(1.minutes) {
                client.query(prompt).toList()
            }
            println("=== Received Messages ===")
            messages.forEach { message ->
                when (message) {
                    is SystemMessage -> println("System: ${message.subtype} - ${message.data}")
                    is AssistantMessage -> println("Assistant: ${message.text}")
                    is ResultMessage -> println("Result: success=${!message.isError}, cost=\$${message.totalCostUsd}, tokens=${message.usage}")
                    else -> println("${message::class.simpleName}: $message")
                }
            }
            println("=======================")

            // Then
            assertTrue(messages.isNotEmpty(), "Should receive at least one message")

            val assistantMessages = messages.filterIsInstance<AssistantMessage>()
            assertTrue(assistantMessages.isNotEmpty(), "Should receive assistant messages")

            val responseText = assistantMessages.first().text

            // Check if we got credit balance error
            if (responseText.contains("Credit balance is too low", ignoreCase = true)) {
                println("ERROR: Test failed due to insufficient credits")
                println("ERROR: Response: $responseText")
                throw AssertionError("Test cannot run: Credit balance is too low. Please add credits to your account.")
            }

            assertTrue(responseText.contains("4"), "Response should contain the answer '4'")
        } catch (e: Exception) {
            println("ERROR: Test failed with exception: ${e.message}")
            println("ERROR: Exception type: ${e::class.simpleName}")
            if (e is net.cyclingbits.claudecode.exceptions.ProcessException) {
                println("ERROR: Process exit code: ${e.exitCode}")
                println("ERROR: Process stderr: ${e.stderr}")
            }
            throw e
        }
    }
}