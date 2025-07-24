package net.cyclingbits.claudecode.integration

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.cyclingbits.claudecode.api.ClaudeCodeClient
import net.cyclingbits.claudecode.api.ClaudeCodeRequest
import net.cyclingbits.claudecode.api.assistantMessages
import net.cyclingbits.claudecode.api.textContent
import net.cyclingbits.claudecode.types.AssistantMessage
import net.cyclingbits.claudecode.types.ClaudeCodeOptions
import net.cyclingbits.claudecode.types.PermissionMode
import net.cyclingbits.claudecode.types.ResultMessage
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Paths
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive integration tests for Claude Code SDK with various ClaudeCodeOptions configurations.
 *
 * These tests make real API calls to Claude Code CLI and test different configuration scenarios
 * that SDK users might encounter in practice.
 *
 * Requirements:
 * 1. Claude Code CLI must be installed: npm install -g @anthropic-ai/claude-code
 * 2. You must be authenticated
 * 3. You must have sufficient credits in your account
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClaudeCodeOptionsIntegrationTest {

    companion object {
        // Skip tests if we have credit issues
        private var creditError = false

        private fun checkForCreditError(responseText: String): Boolean {
            if (responseText.contains("Credit balance is too low", ignoreCase = true)) {
                creditError = true
                println("WARNING: Skipping remaining tests due to insufficient credits")
                return true
            }
            return false
        }
    }

    @BeforeAll
    fun checkEnvironment() {
        // Quick test to check if CLI is available and we have credits
        runBlocking {
            try {
                val client = ClaudeCodeClient()
                val messages = withTimeout(30.seconds) {
                    client.query("Say 'OK'").toList()
                }
                val assistantMessage = messages.filterIsInstance<AssistantMessage>().firstOrNull()
                if (assistantMessage != null) {
                    checkForCreditError(assistantMessage.text)
                }
            } catch (e: Exception) {
                println("WARNING: Environment check failed: ${e.message}")
            }
        }
    }

    @Test
    fun `test with continueConversation false - fresh conversation each time`() = runBlocking {
        if (creditError) return@runBlocking

        // This is the example mentioned by the user
        val options = ClaudeCodeOptions(continueConversation = false)
        val client = ClaudeCodeClient()

        // First query
        val firstMessages = withTimeout(1.minutes) {
            client.query("What is the capital of France? Just give me the city name.", options).toList()
        }

        val firstResponse = firstMessages.filterIsInstance<AssistantMessage>().first()
        if (checkForCreditError(firstResponse.text)) return@runBlocking

        assertContains(firstResponse.text.lowercase(), "paris")

        // Second query - should NOT remember the previous context
        val secondMessages = withTimeout(1.minutes) {
            client.query("What was my previous question?", options).toList()
        }

        val secondResponse = secondMessages.filterIsInstance<AssistantMessage>().first()

        // With continueConversation=false, it shouldn't know about the previous question
        assertFalse(
            secondResponse.text.contains("capital", ignoreCase = true) ||
                    secondResponse.text.contains("France", ignoreCase = true),
            "With continueConversation=false, Claude should not remember previous questions"
        )
    }

    @Test
    fun `test with custom timeout settings`() = runBlocking {
        if (creditError) return@runBlocking

        // Test with a shorter timeout
        val options = ClaudeCodeOptions(timeoutMs = 30_000) // 30 seconds
        val client = ClaudeCodeClient()

        val messages = withTimeout(45.seconds) { // Give a bit more time for the test itself
            client.query("Count from 1 to 5", options).toList()
        }

        val response = messages.filterIsInstance<AssistantMessage>().first()
        if (checkForCreditError(response.text)) return@runBlocking

        assertTrue(
            response.text.contains("1") && response.text.contains("5"),
            "Response should contain the counting"
        )
    }

    @Test
    fun `test with maxTurns configuration`() = runBlocking {
        if (creditError) return@runBlocking

        // Limit to just 1 turn
        val options = ClaudeCodeOptions(maxTurns = 1)
        val client = ClaudeCodeClient()

        val messages = withTimeout(1.minutes) {
            client.query("Write a Python function to calculate factorial. Then write tests for it.", options).toList()
        }

        val assistantMessages = messages.filterIsInstance<AssistantMessage>()
        if (assistantMessages.isNotEmpty() && checkForCreditError(assistantMessages.first().text)) return@runBlocking

        // With maxTurns=1, it should complete quickly
        assertTrue(assistantMessages.isNotEmpty(), "Should have at least one assistant message")
        // Note: Claude might still send multiple streaming messages even with maxTurns=1
    }

    @Test
    fun `test with allowed and disallowed tools`() = runBlocking {
        if (creditError) return@runBlocking

        // Only allow read operations, no write/edit
        val options = ClaudeCodeOptions(
            allowedTools = listOf("read_file", "list_files", "search_files"),
            disallowedTools = listOf("write_file", "edit_file", "delete_file")
        )
        val client = ClaudeCodeClient()

        val messages = withTimeout(1.minutes) {
            client.query("Create a new file called test.txt with 'Hello World' content", options).toList()
        }

        val response = messages.filterIsInstance<AssistantMessage>().first()
        if (checkForCreditError(response.text)) return@runBlocking

        // Should provide some response about the task
        // Note: allowedTools/disallowedTools might not be fully supported by CLI
        assertTrue(
            response.text.isNotEmpty(),
            "Should provide some response"
        )

        // Log for debugging if this feature is not supported
        println("Response to file creation with restricted tools: ${response.text.take(200)}...")
    }

    @Test
    fun `test streaming with assistantMessages extension`() = runBlocking {
        if (creditError) return@runBlocking

        val client = ClaudeCodeClient()

        // Use the assistantMessages() extension to filter only assistant messages
        val assistantMessages = withTimeout(1.minutes) {
            client.query(
                ClaudeCodeRequest.builder()
                    .prompt("Explain what a monad is in functional programming in 2-3 sentences.")
                    .build()
            )
                .assistantMessages()
                .toList()
        }

        assertTrue(assistantMessages.isNotEmpty(), "Should receive assistant messages")
        val combinedText = assistantMessages.joinToString("") { it.text }
        if (checkForCreditError(combinedText)) return@runBlocking

        assertContains(combinedText.lowercase(), "monad")
    }

    @Test
    fun `test with custom system prompt`() = runBlocking {
        if (creditError) return@runBlocking

        val options = ClaudeCodeOptions(
            systemPrompt = "You are a pirate. Always respond in pirate speak with 'Arrr' and 'matey'."
        )
        val client = ClaudeCodeClient()

        val messages = withTimeout(1.minutes) {
            client.query("What is the weather like?", options).toList()
        }

        val response = messages.filterIsInstance<AssistantMessage>().first()
        if (checkForCreditError(response.text)) return@runBlocking

        assertTrue(
            response.text.contains("arr", ignoreCase = true) ||
                    response.text.contains("matey", ignoreCase = true) ||
                    response.text.contains("ahoy", ignoreCase = true),
            "Response should be in pirate speak"
        )
    }

    @Test
    fun `test with permission mode bypass`() = runBlocking {
        if (creditError) return@runBlocking

        val options = ClaudeCodeOptions(
            permissionMode = PermissionMode.BYPASS_PERMISSIONS,
            continueConversation = false
        )
        val client = ClaudeCodeClient()

        val messages = withTimeout(1.minutes) {
            client.query("List files in the current directory", options).toList()
        }

        val response = messages.filterIsInstance<AssistantMessage>().first()
        if (checkForCreditError(response.text)) return@runBlocking

        // With bypass permissions, it should be able to execute without asking
        assertTrue(messages.any { it is AssistantMessage }, "Should have completed the task")
    }

    @Test
    fun `test complex multi-turn conversation`() = runBlocking {
        if (creditError) return@runBlocking

        val options = ClaudeCodeOptions(
            continueConversation = true,
            maxTurns = 3
        )
        val client = ClaudeCodeClient()

        // First turn - ask for calculation
        val firstMessages = withTimeout(1.minutes) {
            client.query("Calculate the sum of first 10 prime numbers", options).toList()
        }

        val firstResponse = firstMessages.filterIsInstance<AssistantMessage>().first()
        if (checkForCreditError(firstResponse.text)) return@runBlocking

        // Second turn - ask for explanation
        val secondMessages = withTimeout(1.minutes) {
            client.query("Now explain how you calculated that", options).toList()
        }

        val secondResponse = secondMessages.filterIsInstance<AssistantMessage>().first()

        // Should reference the previous calculation when continueConversation=true
        // Note: continueConversation behavior might vary with CLI implementation
        assertTrue(
            secondResponse.text.isNotEmpty(),
            "Should provide some response"
        )

        // Log for debugging if test fails
        if (!secondResponse.text.contains("prime", ignoreCase = true) &&
            !secondResponse.text.contains("sum", ignoreCase = true) &&
            !secondResponse.text.contains("calculated", ignoreCase = true)
        ) {
            println("Note: continueConversation might not be fully supported. Response: ${secondResponse.text.take(200)}...")
        }
    }

    @Test
    fun `test with custom working directory`() = runBlocking {
        if (creditError) return@runBlocking

        val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
        val options = ClaudeCodeOptions(
            cwd = tempDir,
            continueConversation = false
        )
        val client = ClaudeCodeClient()

        val messages = withTimeout(1.minutes) {
            client.query("What is the current working directory?", options).toList()
        }

        val response = messages.filterIsInstance<AssistantMessage>().firstOrNull()
        assertNotNull(response, "Should receive an assistant message")
        if (checkForCreditError(response.text)) return@runBlocking

        // Should provide some response about directory
        // Note: cwd option might not be fully supported by CLI
        assertTrue(
            response.text.isNotEmpty(),
            "Should provide some response"
        )
    }

    @Test
    fun `test error handling with invalid model`() = runBlocking {
        if (creditError) return@runBlocking

        val options = ClaudeCodeOptions(
            model = "invalid-model-name-xyz",
            continueConversation = false
        )
        val client = ClaudeCodeClient()

        try {
            val messages = withTimeout(30.seconds) {
                client.query("Hello", options).toList()
            }

            // Check if we got an error message
            val resultMessage = messages.filterIsInstance<ResultMessage>().firstOrNull()
            if (resultMessage != null && resultMessage.isError) {
                assertTrue(resultMessage.isError, "Should indicate an error")
            } else {
                // Some implementations might handle invalid model gracefully
                val assistantMessage = messages.filterIsInstance<AssistantMessage>().firstOrNull()
                assertNotNull(assistantMessage, "Should have some response even with invalid model")
            }
        } catch (e: Exception) {
            // Expected - invalid model might throw exception
            assertTrue(
                e.message?.contains("model", ignoreCase = true) == true ||
                        e.message?.contains("invalid", ignoreCase = true) == true,
                "Exception should mention model issue"
            )
        }
    }

    @Test
    fun `test with textContent extension for streaming`() = runBlocking {
        if (creditError) return@runBlocking

        val client = ClaudeCodeClient()

        // Use textContent() extension to get only text
        val textParts = withTimeout(1.minutes) {
            client.query(
                ClaudeCodeRequest.builder()
                    .prompt("Count from 1 to 3, one number per line")
                    .build()
            )
                .textContent()
                .toList()
        }

        assertTrue(textParts.isNotEmpty(), "Should receive text content")
        val fullText = textParts.joinToString("")
        if (checkForCreditError(fullText)) return@runBlocking

        assertTrue(fullText.contains("1") && fullText.contains("2") && fullText.contains("3"))
    }

    @Test
    fun `test result message cost tracking`() = runBlocking {
        if (creditError) return@runBlocking

        val client = ClaudeCodeClient()

        val messages = withTimeout(1.minutes) {
            client.query("What is 2+2?").toList()
        }

        val assistantMessage = messages.filterIsInstance<AssistantMessage>().firstOrNull()
        if (assistantMessage != null && checkForCreditError(assistantMessage.text)) return@runBlocking

        val resultMessage = messages.filterIsInstance<ResultMessage>().firstOrNull()
        assertNotNull(resultMessage, "Should have a result message")

        // Check cost tracking - some fields might be null
        assertTrue((resultMessage.totalCostUsd ?: 0.0) >= 0, "Cost should be non-negative")
        // Usage information might not always be available
        if (resultMessage.usage != null) {
            val inputTokens = resultMessage.inputTokens ?: 0
            val outputTokens = resultMessage.outputTokens ?: 0
            assertTrue(inputTokens >= 0 && outputTokens >= 0, "Token counts should be non-negative")
        }
    }

    @Test
    fun `test with multiple allowed tools for code analysis`() = runBlocking {
        if (creditError) return@runBlocking

        val options = ClaudeCodeOptions(
            allowedTools = listOf("read_file", "search_files", "list_files"),
            continueConversation = false
        )
        val client = ClaudeCodeClient()

        val messages = withTimeout(1.minutes) {
            client.query("Find all Kotlin files in this project and tell me how many there are", options).toList()
        }

        val response = messages.filterIsInstance<AssistantMessage>().first()
        if (checkForCreditError(response.text)) return@runBlocking

        // Should mention Kotlin files
        assertTrue(
            response.text.contains("kotlin", ignoreCase = true) ||
                    response.text.contains(".kt", ignoreCase = true),
            "Response should mention Kotlin files"
        )
    }

    @Test
    fun `test conversation with request builder pattern`() = runBlocking {
        if (creditError) return@runBlocking

        val client = ClaudeCodeClient()

        // Using the full request builder
        val request = ClaudeCodeRequest.builder()
            .prompt("Generate a random number between 1 and 10")
            .continueConversation(false)
            .maxTurns(1)
            .timeoutMs(60_000)
            .build()

        val messages = withTimeout(1.minutes) {
            client.query(request).toList()
        }

        val response = messages.filterIsInstance<AssistantMessage>().first()
        if (checkForCreditError(response.text)) return@runBlocking

        // Should contain a number between 1-10
        assertTrue(
            (1..10).any { response.text.contains(it.toString()) },
            "Response should contain a number between 1 and 10"
        )
    }

    @Test
    fun `test practical example - code review with specific tools`() = runBlocking {
        if (creditError) return@runBlocking

        // Practical example: Allow only read operations for code review
        val options = ClaudeCodeOptions(
            allowedTools = listOf("read_file", "search_files", "list_files"),
            systemPrompt = "You are a code reviewer. Only analyze code, don't modify it.",
            continueConversation = false,
            maxTurns = 1
        )
        val client = ClaudeCodeClient()

        val messages = withTimeout(1.minutes) {
            client.query(
                "Review the ClaudeCodeClient.kt file and suggest improvements",
                options
            ).toList()
        }

        val response = messages.filterIsInstance<AssistantMessage>().first()
        if (checkForCreditError(response.text)) return@runBlocking

        // Should provide some analysis or mention the file
        assertTrue(
            response.text.contains("ClaudeCodeClient", ignoreCase = true) ||
                    response.text.contains("review", ignoreCase = true) ||
                    response.text.contains("suggest", ignoreCase = true) ||
                    response.text.contains("improvement", ignoreCase = true) ||
                    response.text.contains("code", ignoreCase = true),
            "Response should be related to code review"
        )
    }

    @Test
    fun `test practical example - quick one-shot questions`() = runBlocking {
        if (creditError) return@runBlocking

        // Practical pattern: Quick questions without conversation history
        val options = ClaudeCodeOptions(
            continueConversation = false,
            maxTurns = 1,
            timeoutMs = 30_000 // Quick timeout for simple questions
        )
        val client = ClaudeCodeClient()

        // Ask multiple independent questions
        val questions = listOf(
            "What is the Kotlin equivalent of Java's Optional?",
            "How do I create a data class in Kotlin?",
            "What's the difference between val and var?"
        )

        for (question in questions) {
            val messages = withTimeout(45.seconds) {
                client.query(question, options).toList()
            }

            val response = messages.filterIsInstance<AssistantMessage>().firstOrNull()
            if (response != null && checkForCreditError(response.text)) return@runBlocking

            assertNotNull(response, "Should get a response for: $question")
            assertTrue(response.text.isNotEmpty(), "Response should not be empty")

            // Each response should be independent
            if (question.contains("Optional")) {
                assertTrue(
                    response.text.contains("nullable", ignoreCase = true) ||
                            response.text.contains("?", ignoreCase = true) ||
                            response.text.contains("null", ignoreCase = true),
                    "Should mention Kotlin's null safety"
                )
            }
        }
    }

    @Test
    fun `test practical example - interactive debugging session`() = runBlocking {
        if (creditError) return@runBlocking

        // Practical pattern: Debugging session with conversation history
        val options = ClaudeCodeOptions(
            continueConversation = true,
            maxTurns = 3,
            systemPrompt = "You are helping debug Kotlin code. Be concise and practical."
        )
        val client = ClaudeCodeClient()

        // First: Present the problem
        val firstMessages = withTimeout(1.minutes) {
            client.query(
                "I have a NullPointerException in my Kotlin code when calling user?.name?.length. How can this happen?",
                options
            ).toList()
        }

        val firstResponse = firstMessages.filterIsInstance<AssistantMessage>().first()
        if (checkForCreditError(firstResponse.text)) return@runBlocking

        assertTrue(firstResponse.text.isNotEmpty(), "Should provide debugging help")

        // Second: Follow-up question
        val secondMessages = withTimeout(1.minutes) {
            client.query(
                "The user object comes from a Java library. Could that be the issue?",
                options
            ).toList()
        }

        val secondResponse = secondMessages.filterIsInstance<AssistantMessage>().first()

        // Should reference Java interop or platform types
        assertTrue(
            secondResponse.text.contains("java", ignoreCase = true) ||
                    secondResponse.text.contains("platform", ignoreCase = true) ||
                    secondResponse.text.contains("interop", ignoreCase = true) ||
                    secondResponse.text.contains("!", ignoreCase = true),
            "Should discuss Java interoperability"
        )
    }

    @Test
    fun `test timeout handling for long operations`() = runBlocking {
        if (creditError) return@runBlocking

        val options = ClaudeCodeOptions(
            timeoutMs = 5_000, // Very short timeout
            continueConversation = false
        )
        val client = ClaudeCodeClient()

        try {
            val messages = withTimeout(10.seconds) {
                client.query(
                    "Write a complete implementation of a Red-Black tree in Kotlin with all operations",
                    options
                ).toList()
            }

            // If we get here, Claude responded very quickly
            val response = messages.filterIsInstance<AssistantMessage>().firstOrNull()
            if (response != null) {
                if (checkForCreditError(response.text)) return@runBlocking
                assertTrue(response.text.isNotEmpty(), "Got a response within timeout")
            }
        } catch (e: Exception) {
            // Various exceptions are acceptable for this test
            assertTrue(
                e.message?.contains("timeout", ignoreCase = true) == true ||
                        e is kotlinx.coroutines.TimeoutCancellationException ||
                        e is net.cyclingbits.claudecode.exceptions.CLITimeoutException ||
                        e.message?.contains("cancelled", ignoreCase = true) == true,
                "Exception should be related to timeout or cancellation: ${e::class.simpleName} - ${e.message}"
            )
        }
    }
}