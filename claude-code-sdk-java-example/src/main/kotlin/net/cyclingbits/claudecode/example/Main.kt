package net.cyclingbits.claudecode.example

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import net.cyclingbits.claudecode.ClaudeCode
import net.cyclingbits.claudecode.api.*
import net.cyclingbits.claudecode.types.*

fun main() = runBlocking {
    println("Claude Code SDK Example")
    println("======================")
    
    // Example 1: Simple query
    println("\nExample 1: Simple query")
    val client = ClaudeCode.createClient()
    
    client.query("Hello, Claude! What is 2+2?")
        .onEach { message ->
            when (message) {
                is SystemMessage -> println("System: ${message.subtype}")
                is AssistantMessage -> print(message.text)
                is ResultMessage -> println("\nCompleted in ${message.durationMs}ms")
                is UserMessage -> println("User: ${message.content}")
            }
        }
        .collect()
    
    // Example 2: Query with options
    println("\n\nExample 2: Query with custom options")
    val customClient = ClaudeCode.createClient()
    
    customClient.query("List files in current directory")
        .textContent()
        .collect { text ->
            println(text)
        }
    
    // Example 3: Using Outcome API
    println("\n\nExample 3: Using Outcome API")
    val outcome = client.queryOutcome("What's the weather like?")
    
    outcome
        .onSuccess { messages ->
            println("Success! Got ${messages.size} messages")
            messages.forEach { msg ->
                if (msg is AssistantMessage) {
                    println("Assistant: ${msg.text}")
                }
            }
        }
        .onError { exception ->
            println("Error: ${exception.message}")
        }
        .onTimeout { durationMs ->
            println("Timeout after ${durationMs}ms!")
        }
    
    // Example 4: Java-friendly API
    println("\n\nExample 4: Java-friendly callback API")
    client.queryWithCallback("Tell me a short joke") { message ->
        when (message) {
            is AssistantMessage -> print(message.text)
            is ResultMessage -> println("\nCompleted!")
            else -> {} // Ignore other message types
        }
    }
    
    // No need to wait - queryWithCallback is blocking
    
    println("\nExample completed!")
}