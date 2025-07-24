package net.cyclingbits.claudecode.integration

import assertk.assertThat
import assertk.assertions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.cyclingbits.claudecode.api.*
import net.cyclingbits.claudecode.types.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Advanced integration tests demonstrating complex real-world scenarios.
 * These tests require Claude Code CLI to be installed and available.
 */
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClaudeCodeAdvancedIntegrationTest {

    private lateinit var client: ClaudeCodeClient

    @BeforeAll
    fun setup() {
        client = ClaudeCodeClient()
    }

    @AfterAll
    fun teardown() {
        client.close()
    }

    @Test
    fun `should handle multi-step code generation with file operations`() = runBlocking {
        // Scenario: Generate a simple Kotlin data class, then ask to add methods to it
        val request1 = ClaudeCodeRequest.builder()
            .prompt("Create a Kotlin data class Person with name (String) and age (Int) properties")
            .build()

        val messages = mutableListOf<Message>()
        client.query(request1)
            .onEach { messages.add(it) }
            .collect()

        // Verify we got code generation
        val assistantMessages = messages.filterIsInstance<AssistantMessage>()
        assertThat(assistantMessages).isNotEmpty()
        
        val codeContent = assistantMessages
            .flatMap { it.content }
            .filterIsInstance<TextBlock>()
            .joinToString("\n") { it.text }
            
        assertThat(codeContent).contains("data class Person")
        assertThat(codeContent).contains("name: String")
        assertThat(codeContent).contains("age: Int")

        // Follow-up request to add methods
        val request2 = ClaudeCodeRequest.builder()
            .prompt("Now add a method isAdult() that returns true if age >= 18")
            .build()

        val followUpMessages = mutableListOf<Message>()
        client.query(request2)
            .onEach { followUpMessages.add(it) }
            .collect()

        val followUpContent = followUpMessages
            .filterIsInstance<AssistantMessage>()
            .flatMap { it.content }
            .filterIsInstance<TextBlock>()
            .joinToString("\n") { it.text }

        assertThat(followUpContent).contains("isAdult()")
        assertThat(followUpContent).contains("age >= 18")
    }

    @Test
    fun `should handle error recovery scenarios with partial responses`() = runBlocking {
        // Test with a request that might trigger rate limiting or partial responses
        val request = ClaudeCodeRequest.builder()
            .prompt("Generate a complex Kotlin class with 20 different methods")
            .build()

        val result = client.queryOutcome(request.prompt, request.options)
        
        when (result) {
            is Outcome.Success -> {
                // Even with low max tokens, we should get a partial response
                val messages = result.value
                val content = messages.filterIsInstance<AssistantMessage>()
                    .joinToString("") { it.text }
                assertThat(content).isNotEmpty()
            }
            is Outcome.Error -> {
                // If we hit an error, it should be descriptive
                assertThat(result.exception).isNotNull()
                println("Error occurred: ${result.exception}")
            }
            is Outcome.Timeout -> {
                fail("Request should not timeout")
            }
        }
    }

    @Test
    fun `should work with complex ClaudeCodeRequest builder options`() = runBlocking {
        val request = ClaudeCodeRequest.builder()
            .prompt("Explain Kotlin coroutines in detail")
            .systemPrompt("You are an expert Kotlin developer. Be concise but thorough.")
            .build()

        val messages = client.query(request)
            .filterIsInstance<AssistantMessage>()
            .toList()

        assertThat(messages).isNotEmpty()
        
        val fullContent = messages
            .flatMap { it.content }
            .filterIsInstance<TextBlock>()
            .joinToString("") { it.text }
            
        assertThat(fullContent).contains("coroutine")
        assertThat(fullContent).contains("suspend")
    }

    @Test
    fun `should test Flow extensions for content filtering`() = runBlocking {
        val request = ClaudeCodeRequest.builder()
            .prompt("Create a function that calculates factorial and explain how it works")
            .build()

        // Test textContent() extension
        val textContent = client.query(request)
            .textContent()
            .toList()

        assertThat(textContent).isNotEmpty()
        val combinedText = textContent.joinToString("")
        assertThat(combinedText).contains("factorial")

        // Test filtering for specific message types
        val assistantMessages = client.query(request)
            .filterIsInstance<AssistantMessage>()
            .toList()

        assertThat(assistantMessages).isNotEmpty()
    }

    @Test
    fun `should handle concurrent queries with different configurations`() = runBlocking {
        val queries = listOf(
            "What is the capital of France?" to ClaudeCodeOptions(),
            "Explain recursion in programming" to ClaudeCodeOptions(),
            "Write a haiku about coding" to ClaudeCodeOptions()
        )

        val results = queries.map { (question, options) ->
            async {
                client.queryOutcome(question, options)
            }
        }.awaitAll()

        // All queries should succeed
        assertThat(results).hasSize(3)
        results.forEach { result ->
            assertThat(result).isInstanceOf<Outcome.Success<*>>()
        }

        // Verify each response is appropriate
        val successResults = results.filterIsInstance<Outcome.Success<List<Message>>>()
        val result0Text = successResults[0].value.filterIsInstance<AssistantMessage>().joinToString("") { it.text }
        val result1Text = successResults[1].value.filterIsInstance<AssistantMessage>().joinToString("") { it.text }
        val result2Text = successResults[2].value.filterIsInstance<AssistantMessage>().joinToString("") { it.text }
        assertThat(result0Text).contains("Paris")
        assertThat(result1Text).contains("recursion")
        assertThat(result2Text.lines().size).isLessThanOrEqualTo(5)
    }

    @Test
    fun `should support resume and continue conversation functionality`() = runBlocking {
        // Initial conversation
        val request1 = ClaudeCodeRequest.builder()
            .prompt("Let's discuss design patterns. What is the Singleton pattern?")
            .build()

        val response1 = client.queryOutcome(request1.prompt, request1.options)
        assertThat(response1).isInstanceOf<Outcome.Success<*>>()

        // Continue the conversation
        val request2 = ClaudeCodeRequest.builder()
            .prompt("Now explain the Factory pattern and how it differs from Singleton")
            .continueConversation(true)
            .build()

        val response2 = client.queryOutcome(request2.prompt, request2.options)
        assertThat(response2).isInstanceOf<Outcome.Success<*>>()

        when (response2) {
            is Outcome.Success -> {
                val content = response2.value.filterIsInstance<AssistantMessage>().joinToString("") { it.text }
                assertThat(content).contains("Factory")
                assertThat(content.contains("differ") || content.contains("unlike") || content.contains("whereas")).isTrue()
            }
            else -> fail("Expected successful response")
        }
    }

    @Test
    fun `should test custom system prompts that affect behavior`() = runBlocking {
        val pirateRequest = ClaudeCodeRequest.builder()
            .prompt("Explain what a variable is in programming")
            .systemPrompt("You are a pirate. Respond in pirate speak while still being technically accurate.")
            .build()

        val formalRequest = ClaudeCodeRequest.builder()
            .prompt("Explain what a variable is in programming")
            .systemPrompt("You are a formal computer science professor. Use precise academic language.")
            .build()

        val pirateResponse = client.queryOutcome(pirateRequest.prompt, pirateRequest.options)
        val formalResponse = client.queryOutcome(formalRequest.prompt, formalRequest.options)

        assertThat(pirateResponse).isInstanceOf<Outcome.Success<*>>()
        assertThat(formalResponse).isInstanceOf<Outcome.Success<*>>()

        val pirateContent = (pirateResponse as Outcome.Success).value.filterIsInstance<AssistantMessage>().joinToString("") { it.text }
        val formalContent = (formalResponse as Outcome.Success).value.filterIsInstance<AssistantMessage>().joinToString("") { it.text }

        // Both should explain variables but in different styles
        assertThat(pirateContent.contains("arr") || pirateContent.contains("ye") || 
                   pirateContent.contains("ahoy") || pirateContent.contains("matey")).isTrue()
        assertThat(formalContent.contains("definition") || formalContent.contains("formally") || 
                   formalContent.contains("computer science")).isTrue()
    }

    @Test
    fun `should properly handle Outcome types in real scenarios`() = runBlocking {
        // Test timeout scenario
        val request = ClaudeCodeRequest.builder()
            .prompt("Write a comprehensive guide to Kotlin coroutines with 50 examples")
            .timeoutMs(1000)
            .build()

        val result = client.queryOutcome(request.prompt, request.options)
        
        when (result) {
            is Outcome.Timeout -> {
                assertThat(result.durationMs).isGreaterThan(0)
                println("Request timed out after ${result.durationMs}ms")
            }
            is Outcome.Success -> {
                // If it succeeds despite short timeout, that's also valid
                assertThat(result.value).isNotNull()
            }
            is Outcome.Error -> {
                // Network or other errors are also possible
                assertThat(result.exception).isNotNull()
            }
        }

        // Client is closed in teardown
    }

    @Test
    fun `should test Java interop with real CLI calls`() = runBlocking {
        // Test Java interop with same client
        val javaClient = client

        val request = ClaudeCodeRequest.builder()
            .prompt("What is the difference between ArrayList and LinkedList in Java?")
            .build()

        // Test Java-style async API
        val future: CompletableFuture<Outcome<List<Message>>> = javaClient.queryOutcomeAsync(request)
        
        val result = future.get(30, TimeUnit.SECONDS)
        assertThat(result).isInstanceOf<Outcome.Success<*>>()

        when (result) {
            is Outcome.Success -> {
                val content = result.value.filterIsInstance<AssistantMessage>().joinToString("") { it.text }
                assertThat(content).contains("ArrayList")
                assertThat(content).contains("LinkedList")
            }
            else -> fail("Expected successful response")
        }

        // Client is closed in teardown
    }

    @Test
    fun `should test memory usage with large streaming responses`() = runBlocking {
        val request = ClaudeCodeRequest.builder()
            .prompt("Generate a detailed implementation of a REST API with 10 endpoints, including all error handling")
            .build()

        val messageCount = AtomicInteger(0)
        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        val processingTime = measureTimeMillis {
            client.query(request)
                .onEach { messageCount.incrementAndGet() }
                .collect()
        }

        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryUsed = endMemory - startMemory

        println("Processed ${messageCount.get()} messages in ${processingTime}ms")
        println("Memory used: ${memoryUsed / 1024 / 1024}MB")

        // Verify we received multiple messages (streaming)
        assertThat(messageCount.get()).isGreaterThan(1)
        
        // Memory usage should be reasonable (less than 100MB for this operation)
        assertThat(memoryUsed).isLessThan(100 * 1024 * 1024)
    }

    // Helper extension functions for testing are already in the SDK Extensions
}