# Claude Code SDK for Java/Kotlin

<!-- x-release-please-start-version -->
[![Maven Central](https://img.shields.io/maven-central/v/net.cyclingbits/claude-code-sdk-java)](https://central.sonatype.com/artifact/net.cyclingbits/claude-code-sdk-java/1.2.0)
[![javadoc](https://javadoc.io/badge2/net.cyclingbits/claude-code-sdk-java/javadoc.svg)](https://javadoc.io/doc/net.cyclingbits/claude-code-sdk-java/1.2.0)
<!-- x-release-please-end -->

JVM SDK for interacting with Claude Code CLI, providing both Kotlin (coroutines/Flow) and Java (CompletableFuture) APIs.

## Installation

### Maven
<!-- x-release-please-start-version -->
```xml
<dependency>
    <groupId>net.cyclingbits</groupId>
    <artifactId>claude-code-sdk-java</artifactId>
    <version>1.2.0</version>
</dependency>
```
<!-- x-release-please-end -->

### Gradle
<!-- x-release-please-start-version -->
```gradle
implementation 'net.cyclingbits:claude-code-sdk-java:1.2.0'
```
<!-- x-release-please-end -->

### Gradle (Kotlin DSL)
<!-- x-release-please-start-version -->
```kotlin
implementation("net.cyclingbits:claude-code-sdk-java:1.2.0")
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

## Recommended Settings

For optimal performance and control, we recommend using the following settings:

```kotlin
// Kotlin
val options = ClaudeCodeOptions(
    continueConversation = false,  // Start fresh conversation each time
    permissionMode = PermissionMode.BYPASS_PERMISSIONS,  // Skip permission prompts
    maxTurns = 1,  // Limit to single response
    timeoutMs = 60_000  // 60 second timeout
)

val client = ClaudeCodeClient()
val response = client.query("Your prompt here", options)
```

```java
// Java
ClaudeCodeOptions options = ClaudeCodeOptions.builder()
    .continueConversation(false)
    .permissionMode(PermissionMode.BYPASS_PERMISSIONS)
    .maxTurns(1)
    .timeoutMs(60_000)
    .build();

ClaudeCodeClient client = new ClaudeCodeClient();
client.queryAsync("Your prompt here", options);
```

These settings ensure:
- **Clean sessions**: Each query starts fresh without previous context
- **No interruptions**: Permission prompts are automatically approved
- **Quick responses**: Limited to one turn prevents lengthy conversations
- **Reasonable timeout**: 60 seconds is usually sufficient for most queries

## Advanced Usage

### Using Model Context Protocol (MCP)

The SDK supports MCP servers for extending Claude's capabilities:

```kotlin
// Kotlin Example
import net.cyclingbits.claudecode.api.ClaudeCodeClient
import net.cyclingbits.claudecode.api.ClaudeCodeRequest
import net.cyclingbits.claudecode.types.McpStdioServerConfig

val mcpServers = mapOf(
    "filesystem" to McpStdioServerConfig(
        command = "npx",
        args = listOf("@modelcontextprotocol/server-filesystem", "/path/to/project")
    ),
    "github" to McpStdioServerConfig(
        command = "npx", 
        args = listOf("@modelcontextprotocol/server-github"),
        env = mapOf("GITHUB_TOKEN" to "your-token")
    )
)

val request = ClaudeCodeRequest {
    prompt = "List all files in the project directory"
    mcpServers(mcpServers)
    allowedTools = listOf("mcp__filesystem__*")
}

val client = ClaudeCodeClient()
client.query(request).collect { message ->
    println(message)
}
```

```java
// Java Example
import net.cyclingbits.claudecode.api.ClaudeCodeClient;
import net.cyclingbits.claudecode.api.ClaudeCodeRequest;
import net.cyclingbits.claudecode.types.McpStdioServerConfig;

Map<String, McpServerConfig> mcpServers = new HashMap<>();
mcpServers.put("filesystem", new McpStdioServerConfig(
    "stdio",
    "npx",
    Arrays.asList("@modelcontextprotocol/server-filesystem", "/path/to/project"),
    Collections.emptyMap()
));

ClaudeCodeRequest request = ClaudeCodeRequest.builder()
    .prompt("List all files in the project directory")
    .mcpServers(mcpServers)
    .allowedTools(Arrays.asList("mcp__filesystem__*"))
    .build();

ClaudeCodeClient client = new ClaudeCodeClient();
client.queryAsync(request).thenAccept(result -> {
    // Handle result
});
```

### Extended Thinking

Trigger deeper analysis with thinking keywords:

```kotlin
// Kotlin
val request = ClaudeCodeRequest {
    prompt = "think hard about the best way to optimize this algorithm"
    maxThinkingTokens = 16000 // Increase from default 8000
}
```

### Session Management

Continue or resume conversations:

```kotlin
// Continue the last session
val request = ClaudeCodeRequest {
    prompt = "What was the previous result?"
    continueConversation = true
}

// Resume a specific session
val request = ClaudeCodeRequest {
    prompt = "Let's continue our work"
    resume = "session-id-here"
}
```

### Tool Permissions

Control which tools Claude can use:

```kotlin
val request = ClaudeCodeRequest {
    prompt = "Analyze this codebase"
    allowedTools = listOf("Read", "Grep", "LS")
    disallowedTools = listOf("Write", "Bash")
    permissionMode = PermissionMode.DEFAULT
}
```

### Custom Working Directory

```kotlin
val request = ClaudeCodeRequest {
    prompt = "Run tests"
    cwd = Paths.get("/path/to/project")
    allowedTools = listOf("Bash")
}
```

## Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `allowedTools` | List of allowed tool names | `[]` |
| `disallowedTools` | List of disallowed tool names | `[]` |
| `maxThinkingTokens` | Maximum tokens for thinking | `8000` |
| `systemPrompt` | System prompt to prepend | `null` |
| `appendSystemPrompt` | System prompt to append | `null` |
| `mcpServers` | MCP server configurations | `{}` |
| `permissionMode` | Permission handling mode | `null` |
| `continueConversation` | Continue last conversation | `false` |
| `resume` | Resume specific session ID | `null` |
| `maxTurns` | Maximum conversation turns | `null` |
| `model` | Model to use | `null` |
| `cwd` | Working directory | `null` |
| `timeoutMs` | Query timeout in milliseconds | `300000` |

## Error Handling

```kotlin
try {
    client.query(request).collect { message ->
        println(message)
    }
} catch (e: CLINotFoundException) {
    println("Claude CLI not found. Please install: npm install -g @anthropic-ai/claude-code")
} catch (e: ProcessException) {
    println("Process failed with exit code ${e.exitCode}: ${e.stderr}")
} catch (e: CLITimeoutException) {
    println("Query timed out")
}
```

## License

MIT License