package net.cyclingbits.claudecode.internal.client

import kotlinx.coroutines.flow.*
import net.cyclingbits.claudecode.internal.parser.JsonMessageParser
import net.cyclingbits.claudecode.internal.transport.ProcessTransport
import net.cyclingbits.claudecode.internal.transport.Transport
import net.cyclingbits.claudecode.types.ClaudeCodeOptions
import net.cyclingbits.claudecode.types.Message
import java.nio.file.Path

/**
 * Internal client implementation that orchestrates the communication with Claude CLI.
 * 
 * This class is public to allow access from the main API module, but should not be
 * used directly by end users. Use ClaudeCodeClient instead.
 */
public class InternalClient {
    
    private val parser = JsonMessageParser()
    
    /**
     * Verify that Claude CLI is available and can be executed.
     * 
     * @param cliPath Optional path to Claude CLI
     * @throws CLINotFoundException if CLI is not found
     */
    public suspend fun verifyCliAvailable(cliPath: Path? = null) {
        // This will throw CLINotFoundException if CLI is not found
        ProcessTransport.verifyCliAvailable(cliPath)
    }
    
    /**
     * Process a query through transport and return a flow of messages.
     * 
     * @param prompt The prompt to send to Claude
     * @param options Configuration options
     * @param cliPath Optional path to Claude CLI
     * @return Flow of parsed messages
     */
    public suspend fun processQuery(
        prompt: String,
        options: ClaudeCodeOptions,
        cliPath: Path? = null
    ): Flow<Message> = flow {
        val transport: Transport = ProcessTransport(prompt, options, cliPath)
        
        try {
            transport.connect()
            
            transport.receiveMessages()
                .mapNotNull { jsonObject ->
                    parser.parseMessage(jsonObject)
                }
                .collect { message ->
                    emit(message)
                }
        } finally {
            transport.disconnect()
        }
    }.catch { e ->
        // Re-throw exceptions from transport or parser
        throw e
    }
}