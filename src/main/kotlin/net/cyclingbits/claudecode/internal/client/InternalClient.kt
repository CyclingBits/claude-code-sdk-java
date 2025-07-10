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
 */
internal class InternalClient {
    
    private val parser = JsonMessageParser()
    
    /**
     * Process a query through transport and return a flow of messages.
     * 
     * @param prompt The prompt to send to Claude
     * @param options Configuration options
     * @param cliPath Optional path to Claude CLI
     * @return Flow of parsed messages
     */
    suspend fun processQuery(
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