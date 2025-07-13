package net.cyclingbits.claudecode.exceptions

/**
 * Base exception for all Claude SDK errors.
 * 
 * @property message Error message
 * @property cause Underlying cause (if any)
 */
public open class ClaudeSDKException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exception thrown when CLI connection fails.
 * 
 * @property message Error message
 * @property cause Underlying cause (if any)
 */
public open class CLIConnectionException(
    message: String,
    cause: Throwable? = null
) : ClaudeSDKException(message, cause)

/**
 * Exception thrown when Claude CLI is not found.
 * 
 * @property message Error message with installation instructions
 */
public class CLINotFoundException(
    message: String = """
        Claude Code CLI not found. Please install it using:
        
        npm install -g @anthropic-ai/claude-code
        
        Prerequisites:
        - Node.js 18 or higher
        - npm (comes with Node.js)
        
        After installation, make sure the 'claude' command is available in your PATH.
        For more info: https://docs.anthropic.com/en/docs/claude-code/setup
    """.trimIndent()
) : CLIConnectionException(message)

/**
 * Exception thrown when the CLI process exits with an error.
 * 
 * @property exitCode Process exit code
 * @property stderr Error output from the process
 * @property message Error message
 */
public class ProcessException(
    public val exitCode: Int,
    public val stderr: String,
    message: String = "Claude CLI process exited with code $exitCode: $stderr"
) : ClaudeSDKException(message)

/**
 * Exception thrown when JSON decoding fails.
 * 
 * @property line The line that failed to decode
 * @property message Error message
 * @property cause JSON parsing exception
 */
public class CLIJSONDecodeException(
    public val line: String,
    message: String = "Failed to decode JSON from CLI output: $line",
    cause: Throwable? = null
) : ClaudeSDKException(message, cause)

/**
 * Exception thrown when a timeout occurs.
 * 
 * @property timeoutMs Timeout duration in milliseconds
 * @property message Error message
 */
public class TimeoutException(
    public val timeoutMs: Long,
    message: String = "Operation timed out after ${timeoutMs}ms"
) : ClaudeSDKException(message)

/**
 * Exception thrown when CLI operation times out.
 * 
 * @property message Error message
 */
public class CLITimeoutException(
    message: String
) : ClaudeSDKException(message)

/**
 * Exception thrown when the SDK is not properly configured.
 * 
 * @property message Configuration error message
 */
public class ConfigurationException(
    message: String
) : ClaudeSDKException(message)