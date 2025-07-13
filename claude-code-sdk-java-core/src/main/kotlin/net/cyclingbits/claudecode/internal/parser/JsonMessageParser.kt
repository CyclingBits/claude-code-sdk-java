package net.cyclingbits.claudecode.internal.parser

import kotlinx.serialization.json.*
import net.cyclingbits.claudecode.types.*

/**
 * Parser for JSON messages from Claude CLI.
 */
internal class JsonMessageParser {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Parse a JSON object into a Message.
     * 
     * @param data JSON object from CLI
     * @return Parsed message or null if type is unknown
     */
    fun parseMessage(data: JsonObject): Message? {
        val type = data["type"]?.jsonPrimitive?.content ?: return null
        
        return when (type) {
            "user" -> parseUserMessage(data)
            "assistant" -> parseAssistantMessage(data)
            "system" -> parseSystemMessage(data)
            "result" -> parseResultMessage(data)
            else -> null
        }
    }
    
    private fun parseUserMessage(data: JsonObject): UserMessage? {
        val messageObj = data["message"]?.jsonObject ?: return null
        val content = messageObj["content"]?.jsonPrimitive?.content ?: return null
        
        return UserMessage(content)
    }
    
    private fun parseAssistantMessage(data: JsonObject): AssistantMessage? {
        val messageObj = data["message"]?.jsonObject ?: return null
        val contentArray = messageObj["content"]?.jsonArray ?: return null
        
        val contentBlocks = contentArray.mapNotNull { element ->
            val block = element.jsonObject
            val blockType = block["type"]?.jsonPrimitive?.content ?: return@mapNotNull null
            
            when (blockType) {
                "text" -> {
                    val text = block["text"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    TextBlock(text)
                }
                "tool_use" -> {
                    val id = block["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val name = block["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val input = block["input"]?.jsonObject ?: JsonObject(emptyMap())
                    ToolUseBlock(id, name, input)
                }
                "tool_result" -> {
                    val toolUseId = block["tool_use_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val content = block["content"]
                    val isError = block["is_error"]?.jsonPrimitive?.booleanOrNull
                    ToolResultBlock(toolUseId, content, isError)
                }
                else -> null
            }
        }
        
        return AssistantMessage(contentBlocks)
    }
    
    private fun parseSystemMessage(data: JsonObject): SystemMessage? {
        val subtype = data["subtype"]?.jsonPrimitive?.content ?: return null
        
        // Remove type field to avoid duplication in data
        val dataObj = buildJsonObject {
            data.forEach { (key, value) ->
                if (key != "type") {
                    put(key, value)
                }
            }
        }
        
        return SystemMessage(subtype, dataObj)
    }
    
    private fun parseResultMessage(data: JsonObject): ResultMessage? {
        val subtype = data["subtype"]?.jsonPrimitive?.content ?: return null
        val durationMs = data["duration_ms"]?.jsonPrimitive?.intOrNull ?: return null
        val durationApiMs = data["duration_api_ms"]?.jsonPrimitive?.intOrNull ?: return null
        val isError = data["is_error"]?.jsonPrimitive?.booleanOrNull ?: return null
        val numTurns = data["num_turns"]?.jsonPrimitive?.intOrNull ?: return null
        val sessionId = data["session_id"]?.jsonPrimitive?.content ?: return null
        
        val totalCostUsd = data["total_cost_usd"]?.jsonPrimitive?.doubleOrNull
        val usage = data["usage"]?.jsonObject
        val result = data["result"]?.jsonPrimitive?.content
        
        return ResultMessage(
            subtype = subtype,
            durationMs = durationMs,
            durationApiMs = durationApiMs,
            isError = isError,
            numTurns = numTurns,
            sessionId = sessionId,
            totalCostUsd = totalCostUsd,
            usage = usage,
            result = result
        )
    }
}