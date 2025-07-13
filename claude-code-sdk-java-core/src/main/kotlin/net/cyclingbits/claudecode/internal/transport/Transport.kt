package net.cyclingbits.claudecode.internal.transport

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

/**
 * Abstract transport interface for Claude communication.
 * 
 * This interface defines the contract for different transport implementations
 * (e.g., subprocess, HTTP, WebSocket).
 */
internal interface Transport {
    
    /**
     * Initialize the connection.
     * 
     * @throws net.cyclingbits.claudecode.exceptions.CLIConnectionException if connection fails
     */
    suspend fun connect()
    
    /**
     * Close the connection and release resources.
     */
    suspend fun disconnect()
    
    /**
     * Send a request to Claude.
     * 
     * @param messages List of messages to send
     * @param options Request options
     * @throws net.cyclingbits.claudecode.exceptions.ClaudeSDKException if sending fails
     */
    suspend fun sendRequest(
        messages: List<JsonObject>,
        options: JsonObject
    )
    
    /**
     * Receive messages from Claude as a Flow.
     * 
     * @return Flow of JSON messages
     */
    fun receiveMessages(): Flow<JsonObject>
    
    /**
     * Check if the transport is connected.
     * 
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean
}