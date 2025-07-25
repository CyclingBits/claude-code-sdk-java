package net.cyclingbits.claudecode.api

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.future
import net.cyclingbits.claudecode.exceptions.*
import net.cyclingbits.claudecode.internal.client.InternalClient
import net.cyclingbits.claudecode.types.ClaudeCodeOptions
import net.cyclingbits.claudecode.types.Message
import net.cyclingbits.claudecode.types.Outcome
import java.io.Closeable
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/**
 * Main client for interacting with Claude Code.
 * 
 * This client provides both Kotlin coroutine-based and Java-friendly APIs.
 * 
 * Example usage (Kotlin):
 * ```kotlin
 * ClaudeCodeClient().use { client ->
 *     client.query("Help me write a function").collect { message ->
 *         println(message)
 *     }
 * }
 * ```
 * 
 * Example usage (Java):
 * ```java
 * try (ClaudeCodeClient client = new ClaudeCodeClient()) {
 *     client.queryAsync("Help me write a function").thenAccept(messages -> {
 *         messages.forEach(System.out::println);
 *     });
 * }
 * ```
 */
public class ClaudeCodeClient @JvmOverloads constructor(
    private val cliPath: Path? = null,
    skipVerification: Boolean = false
) : AutoCloseable, Closeable {
    private val internalClient: InternalClient = InternalClient()
    
    /**
     * Coroutine scope for managing async operations.
     * Uses SupervisorJob to prevent failures from cancelling other operations.
     */
    private val scope = CoroutineScope(
        SupervisorJob() + 
        Dispatchers.IO + 
        CoroutineName("ClaudeCodeClient")
    )
    
    init {
        // Verify CLI is available during client creation
        if (!skipVerification) {
            runBlocking {
                internalClient.verifyCliAvailable(cliPath)
            }
        }
    }
    
    companion object {
        /**
         * Creates a test instance with a mocked internal client.
         * This is only for internal testing purposes.
         * @suppress
         */
        @JvmStatic
        internal fun createForTesting(
            cliPath: Path? = null,
            internalClient: InternalClient
        ): ClaudeCodeClient {
            // Create instance with skip verification
            val client = ClaudeCodeClient(cliPath, skipVerification = true)
            
            // Replace the internal client using reflection
            val clientField = ClaudeCodeClient::class.java.getDeclaredField("internalClient")
            clientField.isAccessible = true
            clientField.set(client, internalClient)
            
            return client
        }
    }
    
    /**
     * Send a query to Claude and receive a stream of messages.
     * 
     * This is the main Kotlin API using coroutines and Flow.
     * 
     * @param prompt The prompt to send to Claude
     * @param options Configuration options
     * @return Flow of messages from Claude
     * @throws CLINotFoundException if Claude CLI is not found
     * @throws ProcessException if the CLI process fails
     * @throws CLITimeoutException if the query times out
     * @throws CLIJSONDecodeException if response parsing fails
     */
    @JvmOverloads
    @Throws(CLINotFoundException::class, ProcessException::class, CLITimeoutException::class, CLIJSONDecodeException::class)
    public suspend fun query(
        prompt: String,
        options: ClaudeCodeOptions = ClaudeCodeOptions()
    ): Flow<Message> {
        return internalClient.processQuery(prompt, options, cliPath)
    }
    
    /**
     * Send a query to Claude and collect all messages into a list.
     * 
     * This is useful when you want all messages at once rather than streaming.
     * 
     * @param prompt The prompt to send to Claude
     * @param options Configuration options
     * @return List of all messages
     * @throws CLINotFoundException if Claude CLI is not found
     * @throws ProcessException if the CLI process fails
     * @throws CLITimeoutException if the query times out
     * @throws CLIJSONDecodeException if response parsing fails
     */
    @JvmOverloads
    @Throws(CLINotFoundException::class, ProcessException::class, CLITimeoutException::class, CLIJSONDecodeException::class)
    public suspend fun queryAll(
        prompt: String,
        options: ClaudeCodeOptions = ClaudeCodeOptions()
    ): List<Message> {
        return query(prompt, options).toList()
    }
    
    /**
     * Java-friendly API: Send a query and get a CompletableFuture of all messages.
     * 
     * @param prompt The prompt to send to Claude
     * @param options Configuration options
     * @return CompletableFuture that completes with all messages
     */
    @JvmOverloads
    public fun queryAsync(
        prompt: String,
        options: ClaudeCodeOptions = ClaudeCodeOptions()
    ): CompletableFuture<List<Message>> {
        return scope.future {
            queryAll(prompt, options)
        }
    }
    
    /**
     * Java-friendly API: Send a query and process messages with a callback.
     * 
     * This method blocks until all messages are received.
     * 
     * @param prompt The prompt to send to Claude
     * @param options Configuration options
     * @param onMessage Callback for each message
     */
    @JvmOverloads
    public fun queryWithCallback(
        prompt: String,
        options: ClaudeCodeOptions = ClaudeCodeOptions(),
        onMessage: (Message) -> Unit
    ) {
        runBlocking {
            query(prompt, options).collect { message ->
                onMessage(message)
            }
        }
    }
    
    /**
     * Create a new query using the DSL builder.
     * 
     * Example:
     * ```kotlin
     * client.query {
     *     prompt = "Help me write a function"
     *     options {
     *         allowedTools = listOf("read", "write")
     *         systemPrompt = "You are a Kotlin expert"
     *     }
     * }
     * ```
     * @throws CLINotFoundException if Claude CLI is not found
     * @throws ProcessException if the CLI process fails
     * @throws CLITimeoutException if the query times out
     * @throws CLIJSONDecodeException if response parsing fails
     */
    @Throws(CLINotFoundException::class, ProcessException::class, CLITimeoutException::class, CLIJSONDecodeException::class)
    public suspend fun query(block: QueryBuilder.() -> Unit): Flow<Message> {
        val builder = QueryBuilder().apply(block)
        return query(builder.prompt, builder.buildOptions())
    }
    
    /**
     * Send a query to Claude and get the outcome.
     * 
     * This method collects all messages and returns them wrapped in an Outcome,
     * making it easier to handle success/error cases in Java.
     * 
     * @param prompt The prompt to send to Claude
     * @param options Configuration options
     * @return Outcome containing either success with messages, error, or timeout
     */
    @JvmOverloads
    public suspend fun queryOutcome(
        prompt: String,
        options: ClaudeCodeOptions = ClaudeCodeOptions()
    ): Outcome<List<Message>> {
        val startTime = System.currentTimeMillis()
        val collectedMessages = mutableListOf<Message>()
        
        return try {
            query(prompt, options).collect { message ->
                collectedMessages.add(message)
            }
            
            val duration = System.currentTimeMillis() - startTime
            Outcome.Success(collectedMessages, collectedMessages, duration)
        } catch (e: CLITimeoutException) {
            val duration = System.currentTimeMillis() - startTime
            Outcome.Timeout(duration, collectedMessages)
        } catch (e: ClaudeSDKException) {
            val duration = System.currentTimeMillis() - startTime
            Outcome.Error(e, collectedMessages, duration)
        }
    }
    
    /**
     * Java-friendly API: Send a query and get an Outcome wrapped in CompletableFuture.
     * 
     * @param prompt The prompt to send to Claude
     * @param options Configuration options
     * @return CompletableFuture that completes with an Outcome
     */
    @JvmOverloads
    public fun queryOutcomeAsync(
        prompt: String,
        options: ClaudeCodeOptions = ClaudeCodeOptions()
    ): CompletableFuture<Outcome<List<Message>>> {
        return scope.future {
            queryOutcome(prompt, options)
        }
    }
    
    /**
     * Send a query using a ClaudeCodeRequest.
     * 
     * This is the recommended API for Java users.
     * 
     * @param request The request containing prompt and options
     * @return Flow of messages from Claude
     */
    @Throws(CLINotFoundException::class, ProcessException::class, CLITimeoutException::class, CLIJSONDecodeException::class)
    public suspend fun query(request: ClaudeCodeRequest): Flow<Message> {
        return query(request.prompt, request.options)
    }
    
    /**
     * Java-friendly API: Send a query using ClaudeCodeRequest.
     * 
     * @param request The request containing prompt and options
     * @return CompletableFuture that completes with all messages
     */
    public fun queryAsync(request: ClaudeCodeRequest): CompletableFuture<List<Message>> {
        return queryAsync(request.prompt, request.options)
    }
    
    /**
     * Java-friendly API: Send a query using ClaudeCodeRequest and get an Outcome.
     * 
     * @param request The request containing prompt and options
     * @return CompletableFuture that completes with an Outcome
     */
    public fun queryOutcomeAsync(request: ClaudeCodeRequest): CompletableFuture<Outcome<List<Message>>> {
        return queryOutcomeAsync(request.prompt, request.options)
    }
    
    /**
     * Close the client and release all resources.
     * 
     * This cancels all ongoing coroutines started by this client.
     * After calling close(), the client should not be used anymore.
     */
    override fun close() {
        scope.cancel("Client closed")
    }
}

/**
 * DSL builder for creating queries.
 */
public class QueryBuilder {
    /**
     * The prompt to send to Claude.
     */
    public var prompt: String = ""
    
    private var options = ClaudeCodeOptions()
    
    /**
     * Configure options using DSL.
     */
    public fun options(block: OptionsBuilder.() -> Unit) {
        val builder = OptionsBuilder(options).apply(block)
        options = builder.build()
    }
    
    internal fun buildOptions(): ClaudeCodeOptions = options
}

/**
 * DSL builder for ClaudeCodeOptions.
 */
public class OptionsBuilder(private var options: ClaudeCodeOptions) {
    /**
     * List of allowed tool names.
     */
    public var allowedTools: List<String>
        get() = options.allowedTools
        set(value) { options = options.copy(allowedTools = value) }
    
    /**
     * Maximum thinking tokens.
     */
    public var maxThinkingTokens: Int
        get() = options.maxThinkingTokens
        set(value) { options = options.copy(maxThinkingTokens = value) }
    
    /**
     * System prompt to use.
     */
    public var systemPrompt: String?
        get() = options.systemPrompt
        set(value) { options = options.copy(systemPrompt = value) }
    
    /**
     * Additional system prompt to append.
     */
    public var appendSystemPrompt: String?
        get() = options.appendSystemPrompt
        set(value) { options = options.copy(appendSystemPrompt = value) }
    
    /**
     * List of disallowed tool names.
     */
    public var disallowedTools: List<String>
        get() = options.disallowedTools
        set(value) { options = options.copy(disallowedTools = value) }
    
    /**
     * Model to use.
     */
    public var model: String?
        get() = options.model
        set(value) { options = options.copy(model = value) }
    
    /**
     * Current working directory.
     */
    public var cwd: Path?
        get() = options.cwd
        set(value) { options = options.copy(cwd = value) }
    
    /**
     * Maximum conversation turns.
     */
    public var maxTurns: Int?
        get() = options.maxTurns
        set(value) { options = options.copy(maxTurns = value) }
    
    /**
     * Whether to continue previous conversation.
     */
    public var continueConversation: Boolean
        get() = options.continueConversation
        set(value) { options = options.copy(continueConversation = value) }
    
    /**
     * Resume token for conversation.
     */
    public var resume: String?
        get() = options.resume
        set(value) { options = options.copy(resume = value) }
    
    internal fun build(): ClaudeCodeOptions = options
}