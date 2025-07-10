import kotlinx.coroutines.runBlocking
import net.cyclingbits.claudecode.*
import net.cyclingbits.claudecode.api.*
import java.nio.file.Paths

fun main() = runBlocking {
    // Create client
    val client = claudeCodeClient()
    
    // Example 1: Simple query
    println("=== Example 1: Simple Query ===")
    client.query("Write a hello world function in Kotlin").collect { message ->
        when (message) {
            is AssistantMessage -> println("Assistant: ${message.text}")
            is ResultMessage -> println("Cost: $${message.totalCostUsd}")
            else -> { /* ignore other messages */ }
        }
    }
    
    // Example 2: Using DSL
    println("\n=== Example 2: Using DSL ===")
    client.query {
        prompt = "Help me refactor this code to be more idiomatic"
        options {
            allowedTools = listOf("read", "write", "edit")
            systemPrompt = "You are a Kotlin expert. Focus on idiomatic Kotlin patterns."
            cwd = Paths.get(".")
            maxTurns = 5
        }
    }.textContent().collect { text ->
        println(text)
    }
    
    // Example 3: Using extension functions
    println("\n=== Example 3: Extension Functions ===")
    client.query("Explain coroutines in Kotlin")
        .assistantMessages()
        .collect { message ->
            println("Assistant has ${message.textBlocks.size} text blocks")
            println("Content: ${message.text}")
        }
    
    // Example 4: Collecting all messages
    println("\n=== Example 4: Collect All ===")
    val allMessages = client.queryAll(
        prompt = "What is the weather like?",
        options = ClaudeCodeOptions(
            model = "claude-3-5-sonnet-20241022",
            maxThinkingTokens = 5000
        )
    )
    
    println("Received ${allMessages.size} messages")
    allMessages.forEach { message ->
        println("- ${message::class.simpleName}")
    }
    
    // Example 5: Error handling
    println("\n=== Example 5: Error Handling ===")
    try {
        client.query("This might fail")
            .errorMessages()
            .collect { errorMessage ->
                if (errorMessage is ResultMessage) {
                    println("Error occurred: ${errorMessage.result}")
                }
            }
    } catch (e: Exception) {
        println("Exception: ${e.message}")
    }
}