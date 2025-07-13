package net.cyclingbits.claudecode.internal.transport

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransportTest {
    
    /**
     * Test implementation of Transport interface for testing.
     */
    private class TestTransport : Transport {
        var connected = false
        var disconnectCalled = false
        val sentMessages = mutableListOf<Pair<List<JsonObject>, JsonObject>>()
        private val messageFlow = MutableSharedFlow<JsonObject>()
        
        override suspend fun connect() {
            connected = true
        }
        
        override suspend fun disconnect() {
            connected = false
            disconnectCalled = true
        }
        
        override suspend fun sendRequest(messages: List<JsonObject>, options: JsonObject) {
            if (!connected) {
                throw IllegalStateException("Not connected")
            }
            sentMessages.add(messages to options)
        }
        
        override fun receiveMessages(): Flow<JsonObject> = messageFlow
        
        override fun isConnected(): Boolean = connected
        
        suspend fun emitMessage(message: JsonObject) {
            messageFlow.emit(message)
        }
    }
    
    @Test
    fun `transport should manage connection state`() = runTest {
        val transport = TestTransport()
        
        assertFalse(transport.isConnected())
        
        transport.connect()
        assertTrue(transport.isConnected())
        
        transport.disconnect()
        assertFalse(transport.isConnected())
        assertTrue(transport.disconnectCalled)
    }
    
    @Test
    fun `transport should send requests when connected`() = runTest {
        val transport = TestTransport()
        transport.connect()
        
        val messages = listOf(
            buildJsonObject { put("type", "user"); put("content", "Hello") }
        )
        val options = buildJsonObject { put("model", "claude-3") }
        
        transport.sendRequest(messages, options)
        
        assertEquals(1, transport.sentMessages.size)
        assertEquals(messages, transport.sentMessages[0].first)
        assertEquals(options, transport.sentMessages[0].second)
    }
    
    @Test
    fun `transport should fail to send when not connected`() = runTest {
        val transport = TestTransport()
        
        val messages = listOf(buildJsonObject { put("type", "user") })
        val options = buildJsonObject {}
        
        try {
            transport.sendRequest(messages, options)
            assert(false) { "Should have thrown exception" }
        } catch (e: IllegalStateException) {
            assertEquals("Not connected", e.message)
        }
    }
    
    
    @Test
    fun `transport should handle multiple send requests`() = runTest {
        val transport = TestTransport()
        transport.connect()
        
        val messages1 = listOf(buildJsonObject { put("content", "First") })
        val options1 = buildJsonObject { put("id", "1") }
        
        val messages2 = listOf(buildJsonObject { put("content", "Second") })
        val options2 = buildJsonObject { put("id", "2") }
        
        transport.sendRequest(messages1, options1)
        transport.sendRequest(messages2, options2)
        
        assertEquals(2, transport.sentMessages.size)
        assertEquals("1", transport.sentMessages[0].second["id"]?.toString()?.trim('"'))
        assertEquals("2", transport.sentMessages[1].second["id"]?.toString()?.trim('"'))
    }
    
    @Test
    fun `transport interface should support different implementations`() = runTest {
        // This test verifies that the interface is properly designed
        // to support different transport mechanisms
        
        val transports: List<Transport> = listOf(
            TestTransport(),
            // Future implementations could include:
            // HttpTransport(), WebSocketTransport(), etc.
        )
        
        transports.forEach { transport ->
            assertFalse(transport.isConnected())
            transport.connect()
            assertTrue(transport.isConnected())
            transport.disconnect()
            assertFalse(transport.isConnected())
        }
    }
}