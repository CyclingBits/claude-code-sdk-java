# Claude Code SDK for JVM

A Kotlin/Java SDK for interacting with Claude Code CLI. This SDK provides a type-safe, coroutine-based API for JVM applications.

## Installation

### Gradle (Kotlin DSL)
```kotlin
dependencies {
    implementation("net.cyclingbits:claude-code-sdk-java:0.1.0")
}
```

### Gradle (Groovy)
```groovy
dependencies {
    implementation 'net.cyclingbits:claude-code-sdk-java:0.1.0'
}
```

### Maven
```xml
<dependency>
    <groupId>net.cyclingbits</groupId>
    <artifactId>claude-code-sdk-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Quick Start

> **Prerequisites**: 
> - Java 8 or higher
> - Claude Code CLI installed: `npm install -g @anthropic-ai/claude-code`
> - Authenticated: `claude setup-token`

### Kotlin
```kotlin
suspend fun main() {
    ClaudeCodeClient().use { client ->
        client.query("Help me write a hello world program").collect { message ->
            when (message) {
                is AssistantMessage -> println(message.text)
                is ResultMessage -> println("Completed in ${message.durationMs}ms")
                else -> { /* handle other messages */ }
            }
        }
    }
}
```

### Java
```java
try (ClaudeCodeClient client = new ClaudeCodeClient()) {
    ClaudeCodeOptions options = ClaudeCodeOptions.builder()
        .allowedTools(Arrays.asList("read", "write"))
        .systemPrompt("You are a Java expert")
        .build();
    
    client.queryAsync("Help me write a hello world program", options)
        .thenAccept(messages -> {
            messages.forEach(System.out::println);
        })
        .join();
}
```

## Key Features

- 🚀 **Kotlin Coroutines & Flow** for async streaming
- ☕ **Java compatibility** with CompletableFuture
- 🔧 **Type-safe API** with sealed classes
- 🔒 **Resource management** with AutoCloseable support
- 📚 **DSL builder** for query construction

### DSL Example (Kotlin)
```kotlin
client.query {
    prompt = "Help me refactor this code"
    options {
        allowedTools = listOf("read", "write", "edit")
        maxTurns = 5
        systemPrompt = "You are a refactoring expert"
    }
}
```

### Extension Functions (Kotlin)
```kotlin
client.query("Write a function")
    .assistantMessages()  // Filter assistant messages
    .textContent()        // Extract text content
    .collect { println(it) }
```

## Testing

```bash
# Run tests with coverage
./gradlew test jacocoTestReport

# Generate API documentation
./gradlew dokkaHtml
```

## License

MIT License - see LICENSE file for details