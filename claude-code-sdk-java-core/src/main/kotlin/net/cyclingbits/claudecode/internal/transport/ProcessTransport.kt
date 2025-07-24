package net.cyclingbits.claudecode.internal.transport

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import okio.*
import okio.buffer
import okio.source
import net.cyclingbits.claudecode.exceptions.*
import net.cyclingbits.claudecode.types.ClaudeCodeOptions
import net.cyclingbits.claudecode.types.McpHttpServerConfig
import net.cyclingbits.claudecode.types.McpSSEServerConfig
import net.cyclingbits.claudecode.types.McpServerConfig
import net.cyclingbits.claudecode.types.McpStdioServerConfig
import net.cyclingbits.claudecode.types.PermissionMode
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile

/**
 * Factory interface for creating processes.
 * This abstraction allows for easier testing by enabling process mocking.
 */
internal interface ProcessFactory {
    fun createProcess(command: List<String>, workingDir: File? = null, environment: Map<String, String> = emptyMap()): Process
}

/**
 * Default implementation of ProcessFactory using ProcessBuilder.
 */
internal class DefaultProcessFactory : ProcessFactory {
    override fun createProcess(command: List<String>, workingDir: File?, environment: Map<String, String>): Process {
        val processBuilder = ProcessBuilder(command).apply {
            workingDir?.let { directory(it) }
            environment.forEach { (key, value) -> environment()[key] = value }
            // Redirect stdin to null (matches Python SDK stdin=None)
            redirectInput(ProcessBuilder.Redirect.from(File(if (System.getProperty("os.name").startsWith("Windows")) "NUL" else "/dev/null")))
        }
        return processBuilder.start()
    }
}

/**
 * Process-based transport implementation using Claude CLI.
 */
internal class ProcessTransport(
    private val prompt: String,
    private val options: ClaudeCodeOptions,
    private val cliPath: Path? = null,
    private val processFactory: ProcessFactory = DefaultProcessFactory()
) : Transport {
    
    private var process: Process? = null
    private var stdoutReader: BufferedSource? = null
    private var stderrReader: BufferedSource? = null
    private val _isConnected = MutableStateFlow(false)
    
    companion object {
        private const val MAX_BUFFER_SIZE = 1024 * 1024 // 1MB
        private const val CLAUDE_CODE_ENTRYPOINT = "sdk-java"
        
        // Common CLI locations
        private val CLI_SEARCH_PATHS = listOf(
            System.getProperty("user.home") + "/.claude/local/claude",
            System.getProperty("user.home") + "/.claude/local/node_modules/.bin/claude",
            System.getProperty("user.home") + "/.npm-global/bin/claude",
            "/usr/local/bin/claude",
            System.getProperty("user.home") + "/.local/bin/claude",
            System.getProperty("user.home") + "/node_modules/.bin/claude",
            System.getProperty("user.home") + "/.yarn/bin/claude",
            "/opt/homebrew/bin/claude",
            "C:\\Program Files\\nodejs\\claude.cmd",
            "C:\\Program Files (x86)\\nodejs\\claude.cmd"
        )
        
        /**
         * Verify that Claude CLI is available and can be executed.
         * 
         * @param cliPath Optional path to Claude CLI
         * @throws CLINotFoundException if CLI is not found
         */
        fun verifyCliAvailable(cliPath: Path? = null) {
            val actualPath = cliPath ?: findCli()
            
            // Try to execute the CLI with --version to verify it works
            try {
                val process = ProcessBuilder(actualPath.toString(), "--version")
                    .start()
                
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw CLINotFoundException(
                        """
                        Claude CLI found at $actualPath but failed to execute.
                        
                        Please ensure:
                        1. The CLI is properly installed
                        2. You have execution permissions
                        3. Try reinstalling: npm install -g @anthropic-ai/claude-code
                        """.trimIndent()
                    )
                }
            } catch (e: Exception) {
                if (e is CLINotFoundException) throw e
                
                throw CLINotFoundException(
                    """
                    Failed to execute Claude CLI at $actualPath: ${e.message}
                    
                    Please ensure:
                    1. Node.js 18+ is installed
                    2. Install Claude Code: npm install -g @anthropic-ai/claude-code
                    3. The 'claude' command is in your PATH
                    """.trimIndent()
                )
            }
        }
        
        private fun findCli(): Path {
            // First, try to find in PATH
            val pathEnv = System.getenv("PATH") ?: ""
            val pathDirs = pathEnv.split(if (System.getProperty("os.name").startsWith("Windows")) ";" else ":")
            
            for (dir in pathDirs) {
                val claudePath = Paths.get(dir, "claude")
                if (claudePath.exists() && claudePath.isRegularFile() && claudePath.isExecutable()) {
                    return claudePath
                }
                // Windows
                val claudeCmdPath = Paths.get(dir, "claude.cmd")
                if (claudeCmdPath.exists() && claudeCmdPath.isRegularFile()) {
                    return claudeCmdPath
                }
            }
            
            // Search in common locations
            for (pathStr in CLI_SEARCH_PATHS) {
                val path = Paths.get(pathStr)
                if (path.exists() && path.isRegularFile()) {
                    return path
                }
            }
            
            // Check if node is installed
            val nodeInstalled = try {
                ProcessBuilder("node", "--version").start().waitFor() == 0
            } catch (e: Exception) {
                false
            }
            
            if (!nodeInstalled) {
                throw CLINotFoundException(
                    """
                    Claude Code requires Node.js, which is not installed.
                    
                    Install Node.js from: https://nodejs.org/
                    
                    After installing Node.js, install Claude Code:
                      npm install -g @anthropic-ai/claude-code
                    """.trimIndent()
                )
            }
            
            throw CLINotFoundException()
        }
    }
    
    private fun buildCommand(): List<String> = buildList {
        val actualCliPath = cliPath ?: ProcessTransport.findCli()
        add(actualCliPath.toString())
        addAll(buildBaseFlags())
        addAll(buildSystemPromptFlags())
        addAll(buildToolFlags())
        addAll(buildExecutionFlags())
        addAll(buildConversationFlags())
        addAll(buildMcpFlags())
        addAll(buildFinalArguments())
    }
    
    private fun buildBaseFlags(): List<String> = listOf(
        "--output-format", "stream-json",
        "--verbose"
    )
    
    private fun buildSystemPromptFlags(): List<String> = buildList {
        options.systemPrompt?.let {
            add("--system-prompt")
            add(it)
        }
        
        options.appendSystemPrompt?.let {
            add("--append-system-prompt")
            add(it)
        }
    }
    
    private fun buildToolFlags(): List<String> = buildList {
        if (options.allowedTools.isNotEmpty()) {
            add("--allowedTools")
            add(options.allowedTools.joinToString(","))
        }
        
        if (options.disallowedTools.isNotEmpty()) {
            add("--disallowedTools")
            add(options.disallowedTools.joinToString(","))
        }
        
        options.permissionPromptToolName?.let {
            add("--permission-prompt-tool")
            add(it)
        }
        
        options.permissionMode?.let {
            add("--permission-mode")
            add(permissionModeToString(it))
        }
    }
    
    private fun buildExecutionFlags(): List<String> = buildList {
        options.maxTurns?.let {
            add("--max-turns")
            add(it.toString())
        }
        
        options.model?.let {
            add("--model")
            add(it)
        }
    }
    
    private fun buildConversationFlags(): List<String> = buildList {
        if (options.continueConversation) {
            add("--continue")
        }
        
        options.resume?.let {
            add("--resume")
            add(it)
        }
    }
    
    private fun buildMcpFlags(): List<String> = buildList {
        if (options.mcpServers.isNotEmpty()) {
            add("--mcp-config")
            add(buildMcpConfig().toString())
        }
    }
    
    private fun buildFinalArguments(): List<String> = listOf(
        "-p",  // Use short form as it's more reliable
        prompt
    )
    
    private fun permissionModeToString(mode: PermissionMode): String = when (mode) {
        PermissionMode.DEFAULT -> "default"
        PermissionMode.ACCEPT_EDITS -> "acceptEdits"
        PermissionMode.BYPASS_PERMISSIONS -> "bypassPermissions"
    }
    
    private fun buildMcpConfig(): JsonObject = buildJsonObject {
        putJsonObject("mcpServers") {
            options.mcpServers.forEach { (name, config) ->
                putJsonObject(name) {
                    serializeMcpServerConfig(config)
                }
            }
        }
    }
    
    private fun JsonObjectBuilder.serializeMcpServerConfig(config: McpServerConfig) {
        when (config) {
            is McpStdioServerConfig -> {
                config.type?.let { put("type", it) }
                put("command", config.command)
                if (config.args.isNotEmpty()) {
                    putJsonArray("args") {
                        config.args.forEach { add(it) }
                    }
                }
                if (config.env.isNotEmpty()) {
                    putJsonObject("env") {
                        config.env.forEach { (k, v) -> put(k, v) }
                    }
                }
            }
            is McpSSEServerConfig -> {
                put("type", config.type)
                put("url", config.url)
                if (config.headers.isNotEmpty()) {
                    putJsonObject("headers") {
                        config.headers.forEach { (k, v) -> put(k, v) }
                    }
                }
            }
            is McpHttpServerConfig -> {
                put("type", config.type)
                put("url", config.url)
                if (config.headers.isNotEmpty()) {
                    putJsonObject("headers") {
                        config.headers.forEach { (k, v) -> put(k, v) }
                    }
                }
            }
        }
    }
    
    override suspend fun connect() {
        if (process != null) {
            return
        }
        
        val command = buildCommand()
        val environment = mapOf("CLAUDE_CODE_ENTRYPOINT" to CLAUDE_CODE_ENTRYPOINT)
        
        try {
            process = processFactory.createProcess(
                command = command,
                workingDir = options.cwd?.toFile(),
                environment = environment
            )
            
            stdoutReader = process!!.inputStream.source().buffer()
            stderrReader = process!!.errorStream.source().buffer()
            
            _isConnected.value = true
        } catch (e: Exception) {
            when {
                options.cwd != null && !options.cwd.exists() -> {
                    throw CLIConnectionException(
                        "Working directory does not exist: ${options.cwd}",
                        e
                    )
                }
                e.message?.contains("Cannot run program") == true -> {
                    throw CLINotFoundException()
                }
                else -> {
                    throw CLIConnectionException("Failed to start Claude CLI process", e)
                }
            }
        }
    }
    
    override suspend fun disconnect() {
        _isConnected.value = false
        stdoutReader?.close()
        stderrReader?.close()
        process?.destroyForcibly()
        process = null
    }
    
    override suspend fun sendRequest(messages: List<JsonObject>, options: JsonObject) {
        // For subprocess transport, the request is sent via command line args
        // No additional sending is needed after connect
    }
    
    override fun receiveMessages(): Flow<JsonObject> = flow {
        val reader = stdoutReader ?: throw IllegalStateException("Not connected")
        
        coroutineScope {
            // Launch stderr collector
            val stderrJob = launch {
                collectStderr()
            }
            
            try {
                withTimeout(options.timeoutMs) {
                    while (isActive && isConnected()) {
                        val line = withContext(Dispatchers.IO) {
                            stdoutReader?.readUtf8Line()
                        }
                        
                        if (line == null) {
                            break
                        }
                        
                        if (line.isBlank()) {
                            continue
                        }
                        
                        try {
                            val json = Json.parseToJsonElement(line).jsonObject
                            emit(json)
                        } catch (e: Exception) {
                            throw CLIJSONDecodeException(line, cause = e)
                        }
                    }
                    
                    // Check if process has finished with error
                    val exitCode = try {
                        process?.exitValue()
                    } catch (e: IllegalThreadStateException) {
                        null
                    }
                    
                    if (exitCode != null && exitCode != 0) {
                        stderrJob.join()
                        val stderr = collectStderr()
                        throw ProcessException(exitCode, stderr)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                process?.destroyForcibly()
                throw CLITimeoutException("Query timed out after ${options.timeoutMs}ms")
            } catch (e: Exception) {
                throw e
            } finally {
                stderrJob.cancelAndJoin()
            }
        }
    }.flowOn(Dispatchers.IO)
    
    private suspend fun collectStderr(): String = withContext(Dispatchers.IO) {
        val reader = stderrReader ?: return@withContext ""
        val buffer = StringBuilder()
        
        try {
            while (isActive) {
                val line = reader.readUtf8Line() ?: break
                buffer.append(line).append("\n")
                
                if (buffer.length > MAX_BUFFER_SIZE) {
                    buffer.delete(0, buffer.length - MAX_BUFFER_SIZE)
                }
            }
        } catch (e: Exception) {
            // Ignore errors in stderr collection
        }
        
        buffer.toString()
    }
    
    override fun isConnected(): Boolean {
        return _isConnected.value && process?.isAlive == true
    }
}