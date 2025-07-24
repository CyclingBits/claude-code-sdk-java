package net.cyclingbits.claudecode.api;

import net.cyclingbits.claudecode.exceptions.ClaudeSDKException;
import net.cyclingbits.claudecode.types.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple Java API tests for ClaudeCodeClient that don't require mocking.
 * These tests focus on the builder patterns and API structure.
 */
@DisplayName("ClaudeCodeClient Java API Tests")
public class ClaudeCodeClientJavaTest {
    
    @Test
    @DisplayName("ClaudeCodeRequest builder should create valid requests")
    void testClaudeCodeRequestBuilder() {
        // Given
        String prompt = "Create a microservice";
        List<String> allowedTools = Arrays.asList("read", "write", "edit");
        List<String> disallowedTools = Arrays.asList("delete");
        String systemPrompt = "You are a Spring Boot expert";
        String model = "claude-3-opus-20240229";
        
        // When
        ClaudeCodeRequest request = ClaudeCodeRequest.builder()
            .prompt(prompt)
            .allowedTools(allowedTools)
            .disallowedTools(disallowedTools)
            .systemPrompt(systemPrompt)
            .appendSystemPrompt("Follow best practices")
            .model(model)
            .maxThinkingTokens(15000)
            .maxTurns(10)
            .timeoutMs(60000)
            .continueConversation(true)
            .resume("session-123")
            .build();
        
        // Then
        assertEquals(prompt, request.getPrompt());
        ClaudeCodeOptions options = request.getOptions();
        assertEquals(allowedTools, options.getAllowedTools());
        assertEquals(disallowedTools, options.getDisallowedTools());
        assertEquals(systemPrompt, options.getSystemPrompt());
        assertEquals("Follow best practices", options.getAppendSystemPrompt());
        assertEquals(model, options.getModel());
        assertEquals(15000, options.getMaxThinkingTokens());
        assertEquals(Integer.valueOf(10), options.getMaxTurns());
        assertEquals(60000, options.getTimeoutMs());
        assertTrue(options.getContinueConversation());
        assertEquals("session-123", options.getResume());
    }
    
    @Test
    @DisplayName("ClaudeCodeRequest builder should throw on empty prompt")
    void testClaudeCodeRequestBuilderEmptyPrompt() {
        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            ClaudeCodeRequest.builder().build();
        });
    }
    
    @Test
    @DisplayName("ClaudeCodeOptions builder should create valid options")
    void testClaudeCodeOptionsBuilder() {
        // When
        ClaudeCodeOptions options = ClaudeCodeOptions.builder()
            .allowedTools(Arrays.asList("read", "write"))
            .disallowedTools(Arrays.asList("delete"))
            .systemPrompt("Be helpful")
            .appendSystemPrompt("And concise")
            .model("claude-3-sonnet-20240229")
            .maxThinkingTokens(10000)
            .maxTurns(5)
            .timeoutMs(30000)
            .continueConversation(false)
            .resume("prev-session")
            .build();
        
        // Then
        assertEquals(Arrays.asList("read", "write"), options.getAllowedTools());
        assertEquals(Arrays.asList("delete"), options.getDisallowedTools());
        assertEquals("Be helpful", options.getSystemPrompt());
        assertEquals("And concise", options.getAppendSystemPrompt());
        assertEquals("claude-3-sonnet-20240229", options.getModel());
        assertEquals(10000, options.getMaxThinkingTokens());
        assertEquals(Integer.valueOf(5), options.getMaxTurns());
        assertEquals(30000, options.getTimeoutMs());
        assertFalse(options.getContinueConversation());
        assertEquals("prev-session", options.getResume());
    }
    
    @Test
    @DisplayName("AssistantMessage should provide convenient accessors")
    void testAssistantMessageAccessors() {
        // Given
        List<ContentBlock> blocks = Arrays.asList(
            new TextBlock("First text"),
            new ToolUseBlock("tool-1", "read_file", new HashMap<>()),
            new TextBlock("Second text"),
            new ToolResultBlock("tool-1", null, false)
        );
        
        AssistantMessage message = new AssistantMessage(blocks);
        
        // Then
        assertEquals(2, message.getTextBlocks().size());
        assertEquals("First text", message.getTextBlocks().get(0).getText());
        assertEquals("Second text", message.getTextBlocks().get(1).getText());
        
        assertEquals(1, message.getToolUseBlocks().size());
        assertEquals("read_file", message.getToolUseBlocks().get(0).getName());
        
        assertEquals(1, message.getToolResultBlocks().size());
        assertEquals("tool-1", message.getToolResultBlocks().get(0).getToolUseId());
        
        assertEquals("First textSecond text", message.getText());
    }
    
    @Test
    @DisplayName("ResultMessage should provide token accessors")
    void testResultMessageTokenAccessors() {
        // Given
        // Create ResultMessage without usage info first
        ResultMessage messageWithoutUsage = new ResultMessage(
            "success",
            5000,
            4500,
            false,
            3,
            "session-123",
            0.15,
            null,
            "Operation completed"
        );
        
        // Then - without usage data
        assertNull(messageWithoutUsage.getInputTokens());
        assertNull(messageWithoutUsage.getOutputTokens());
        assertNull(messageWithoutUsage.getTotalTokens());
        assertEquals(0.15, messageWithoutUsage.getTotalCostUsd());
        assertEquals("Operation completed", messageWithoutUsage.getResult());
    }
    
    @Test
    @DisplayName("Outcome Success should have correct accessors")
    void testOutcomeSuccess() {
        // Given
        List<Message> messages = Arrays.asList(
            new UserMessage("Test"),
            new AssistantMessage(Collections.singletonList(new TextBlock("Response")))
        );
        
        Outcome.Success<List<Message>> success = new Outcome.Success<>(messages, messages, 1000L);
        
        // Then
        assertEquals(messages, success.getValue());
        assertEquals(messages, success.getMessages());
        assertEquals(1000L, success.getDurationMs());
    }
    
    @Test
    @DisplayName("Outcome Error should have correct accessors")
    void testOutcomeError() {
        // Given
        ClaudeSDKException exception = new ClaudeSDKException("Test error", new RuntimeException("Cause"));
        List<Message> partialMessages = Collections.singletonList(new UserMessage("Partial"));
        
        Outcome.Error error = new Outcome.Error(exception, partialMessages, 500L);
        
        // Then
        assertEquals(exception, error.getException());
        assertEquals(partialMessages, error.getPartialMessages());
        assertEquals(500L, error.getDurationMs());
    }
    
    @Test
    @DisplayName("Outcome Timeout should have correct accessors")
    void testOutcomeTimeout() {
        // Given
        List<Message> partialMessages = Collections.singletonList(new UserMessage("Partial"));
        
        Outcome.Timeout timeout = new Outcome.Timeout(5000L, partialMessages);
        
        // Then
        assertEquals(partialMessages, timeout.getPartialMessages());
        assertEquals(5000L, timeout.getDurationMs());
    }
}