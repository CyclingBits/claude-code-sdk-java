# Claude Code SDK for Java

[![Maven Central](https://img.shields.io/maven-central/v/net.cyclingbits/claude-code-sdk-java)](https://central.sonatype.com/artifact/net.cyclingbits/claude-code-sdk-java/1.0.1)
[![javadoc](https://javadoc.io/badge2/net.cyclingbits/claude-code-sdk-java/javadoc.svg)](https://javadoc.io/doc/net.cyclingbits/claude-code-sdk-java/1.0.1)

JVM SDK for interacting with Claude Code CLI, providing both Kotlin (coroutines/Flow) and Java (CompletableFuture) APIs.

## Installation

### Maven
<!-- x-release-please-start-version -->
```xml
<dependency>
    <groupId>net.cyclingbits</groupId>
    <artifactId>claude-code-sdk-java</artifactId>
    <version>1.0.1</version>
</dependency>
```
<!-- x-release-please-end -->

### Gradle
<!-- x-release-please-start-version -->
```gradle
implementation 'net.cyclingbits:claude-code-sdk-java:1.0.1'
```
<!-- x-release-please-end -->

### Gradle (Kotlin DSL)
<!-- x-release-please-start-version -->
```kotlin
implementation("net.cyclingbits:claude-code-sdk-java:1.0.1")
```
<!-- x-release-please-end -->

## Requirements

- Java 8 or higher
- Claude Code CLI installed: `npm install -g @anthropic-ai/claude-code`
- Authenticated with Claude: `claude setup-token`

## Quick Start

### Kotlin
```kotlin
import net.cyclingbits.claudecode.api.ClaudeCodeClient
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = ClaudeCodeClient()
    val messages = client.query("What is 2 + 2?").toList()
    
    messages.forEach { message ->
        println(message)
    }
}
```

### Java
```java
import net.cyclingbits.claudecode.api.ClaudeCodeClient;
import net.cyclingbits.claudecode.types.AssistantMessage;
import net.cyclingbits.claudecode.types.Outcome;

public class Example {
    public static void main(String[] args) {
        ClaudeCodeClient client = new ClaudeCodeClient();
        
        Outcome<AssistantMessage> result = client.queryAsync("What is 2 + 2?")
            .join();
        
        if (result instanceof Outcome.Success) {
            AssistantMessage message = ((Outcome.Success<AssistantMessage>) result).getValue();
            System.out.println(message.getText());
        }
    }
}
```

## License

MIT License