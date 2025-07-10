# Claude Code SDK for JVM

A Kotlin/Java SDK for interacting with Claude Code CLI. This SDK provides a type-safe, coroutine-based API for JVM applications to interact with Claude through the command-line interface.

## Status: In Development

This SDK is currently under active development. The API may change before the 1.0 release.

## Features

- 🚀 **Kotlin Coroutines & Flow** for async streaming
- 🔧 **Type-safe API** with sealed classes and data classes
- ☕ **Java compatibility** with builder patterns and convenience methods
- 📦 **Minimal dependencies** - only essential libraries
- 🎯 **Explicit API mode** for better library design
- ⏱️ **Configurable timeout** with proper cleanup
- 🧪 **80%+ code coverage** with JaCoCo

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("net.cyclingbits:claude-code-sdk-java:0.1.0-SNAPSHOT")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'net.cyclingbits:claude-code-sdk-java:0.1.0-SNAPSHOT'
}
```

### Maven

```xml
<dependency>
    <groupId>net.cyclingbits</groupId>
    <artifactId>claude-code-sdk-java</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

> **Note**: Make sure you have set up authentication first: `claude setup-token`

### Kotlin

```kotlin
suspend fun main() {
    val client = ClaudeCodeClient()
    
    // Simple query
    client.query("Help me write a hello world program").collect { message ->
        when (message) {
            is AssistantMessage -> println(message.text)
            is ResultMessage -> println("Query completed in ${message.durationMs}ms")
            else -> { /* handle other message types */ }
        }
    }
    
    // With options
    val options = ClaudeCodeOptions(
        allowedTools = listOf("read", "write"),
        systemPrompt = "You are a Kotlin expert",
        model = "claude-sonnet-4-20250522", // Claude 4 Sonnet
        timeoutMs = 60_000 // 1 minute timeout
    )
    
    client.query("Create a data class for a User", options).collect { message ->
        println(message)
    }
}
```

### Java

```java
import net.cyclingbits.claudecode.api.ClaudeCodeClient;
import net.cyclingbits.claudecode.types.*;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class Example {
    public static void main(String[] args) {
        ClaudeCodeClient client = new ClaudeCodeClient();
        
        // Build options with builder pattern
        ClaudeCodeOptions options = ClaudeCodeOptions.builder()
            .allowedTools(Arrays.asList("read", "write"))
            .systemPrompt("You are a Java expert")
            .model("claude-sonnet-4-20250522") // Claude 4 Sonnet
            .timeoutMs(60000) // 1 minute timeout
            .build();
        
        // Async API with CompletableFuture
        CompletableFuture<List<Message>> future = client.queryAsync(
            "Help me write a hello world program", 
            options
        );
        
        future.thenAccept(messages -> {
            for (Message message : messages) {
                if (message instanceof AssistantMessage) {
                    System.out.println(((AssistantMessage) message).getText());
                }
            }
        }).join();
        
        // Callback API
        client.queryWithCallback(
            "Create a simple Java class",
            options,
            message -> System.out.println("Received: " + message),
            error -> System.err.println("Error: " + error.getMessage())
        );
    }
}
```

## Requirements

- Java 8 or higher
- Claude Code CLI installed and available in PATH (`npm install -g @anthropic-ai/claude-code`)
- Kotlin 2.2.0+ (for Kotlin projects)
- Active Claude Code subscription (authenticated via CLI)

## Architecture

The SDK is structured into several packages:

- `api` - Public API and client
- `types` - Data types and messages
- `exceptions` - Exception hierarchy
- `internal` - Internal implementation (not part of public API)

## Development Status

✅ **All phases completed!**

- [x] Phase 1: Basic types and configuration
- [x] Phase 2: Transport layer implementation
- [x] Phase 3: Parser and internal client
- [x] Phase 4: Public API completion
- [x] Phase 5: Testing and documentation

## Features

The SDK includes:
- 🔄 Complete type system with sealed interfaces for messages
- 🚀 Process-based transport for CLI communication
- 📋 JSON message parser using kotlinx.serialization
- ⚡ Coroutine-based streaming API with Flow
- ☕ Java-friendly API with CompletableFuture
- 🏗️ DSL builder for query construction
- 🔧 Extension functions for convenient filtering
- 🌍 Cross-platform support (Windows, macOS, Linux)
- 🧪 Comprehensive test suite
- 📚 Full API documentation

## Authentication

This SDK is a wrapper around the Claude Code CLI and relies on the CLI's authentication. You must authenticate through the CLI before using the SDK:

```bash
# Set up authentication token (required before using the SDK)
claude setup-token

# Verify authentication works
echo "test" | claude --print
```

The SDK **does not** handle authentication directly:
- ❌ No API key support
- ❌ No programmatic login
- ✅ Uses existing CLI authentication
- ✅ Supports all subscription features available to your account

## Model Selection

You can choose between available Claude models using the `model` option:

### Kotlin
```kotlin
val options = ClaudeCodeOptions(
    model = "claude-sonnet-4-20250522",     // Claude 4 Sonnet (balanced)
    // model = "claude-opus-4-20250522",    // Claude 4 Opus (most capable)
    // model = "claude-3-5-sonnet-20241022", // Claude 3.5 Sonnet (previous gen)
    allowedTools = listOf("read", "write")
)

client.query("Help me code", options).collect { /* ... */ }
```

### Java
```java
ClaudeCodeOptions options = ClaudeCodeOptions.builder()
    .model("claude-sonnet-4-20250522")     // Claude 4 Sonnet (balanced)
    // .model("claude-opus-4-20250522")    // Claude 4 Opus (most capable)
    // .model("claude-3-5-sonnet-20241022") // Claude 3.5 Sonnet (previous gen)
    .allowedTools(Arrays.asList("read", "write"))
    .build();
```

### Available Models

#### Claude 4 Family (Latest)
- `claude-opus-4-20250522` - Claude 4 Opus (most intelligent, Level 3 safety rating, requires Opus access)
- `claude-sonnet-4-20250522` - Claude 4 Sonnet (balanced performance/cost, recommended)

#### Claude 3.5 Family
- `claude-3-5-sonnet-20241022` - Claude 3.5 Sonnet (previous generation, still available)

#### Claude 3 Family
- `claude-3-opus-20240229` - Claude 3 Opus
- `claude-3-sonnet-20240229` - Claude 3 Sonnet
- `claude-3-haiku-20240307` - Claude 3 Haiku (fastest, most economical)

Note: Model availability depends on your subscription level. Claude 4 models support 200K token context and advanced features like hybrid reasoning modes.

## Advanced Usage

### Extension Functions (Kotlin)

```kotlin
// Filter and process messages
client.query("Write a function")
    .assistantMessages()  // Only assistant messages
    .textContent()        // Extract text content
    .collect { text ->
        println(text)
    }

// Extract tool uses
client.query("Read the config file")
    .toolUses()
    .filter { it.name == "read_file" }
    .collect { toolUse ->
        println("Reading file: ${toolUse.input}")
    }
```

### DSL Query Builder (Kotlin)

```kotlin
client.queryDsl {
    prompt = "Help me refactor this code"
    options {
        allowedTools = listOf("read", "write", "edit")
        maxTurns = 5
        systemPrompt = "You are a refactoring expert"
    }
}.collect { message ->
    // Process messages
}
```

### Error Handling

```kotlin
try {
    client.query("Help me").collect { message ->
        // Process
    }
} catch (e: CLINotFoundException) {
    println("Claude CLI not found. Please install it.")
} catch (e: CLITimeoutException) {
    println("Query timed out")
} catch (e: ProcessException) {
    println("CLI process failed: ${e.stderr}")
}
```

## API Documentation

Full API documentation can be generated using:

```bash
./gradlew dokkaHtml
```

The documentation will be available in `build/dokka/html/index.html`.

## Project Structure

```
claudecode/
├── api/           # Public API (ClaudeCodeClient, Extensions)
├── types/         # Data types (Messages, Options, Blocks)
├── exceptions/    # Exception hierarchy
└── internal/      # Internal implementation
    ├── client/    # Internal client logic
    ├── parser/    # JSON message parser
    └── transport/ # Process transport layer
```

## Testing

### Unit Tests
Run unit tests with coverage:

```bash
./gradlew test jacocoTestReport
```

View coverage report in `build/reports/jacoco/test/html/index.html`.

### Integration Tests
Integration tests make real API calls to Claude Code CLI and are disabled by default.

To run integration tests:
```bash
./gradlew test -Drun.integration.tests=true --tests "*IntegrationTest"
```

See [INTEGRATION_TESTS.md](INTEGRATION_TESTS.md) for detailed instructions.

## Related Projects

- [Claude Code CLI](https://github.com/anthropics/claude-code) - The CLI this SDK wraps
- [Anthropic SDK Java](https://github.com/anthropics/anthropic-sdk-java) - Official Java SDK for Anthropic API
- [Claude Code SDK Python](https://github.com/anthropics/claude-code-sdk-python) - Python version of this SDK

## License

MIT License - see LICENSE file for details