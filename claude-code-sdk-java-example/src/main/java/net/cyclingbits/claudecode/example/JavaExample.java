package net.cyclingbits.claudecode.example;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.cyclingbits.claudecode.ClaudeCode;
import net.cyclingbits.claudecode.api.ClaudeCodeClient;
import net.cyclingbits.claudecode.api.ClaudeCodeRequest;
import net.cyclingbits.claudecode.exceptions.ClaudeSDKException;
import net.cyclingbits.claudecode.types.*;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive Java example demonstrating various ways to use Claude Code SDK.
 * <p>
 * This example shows:
 * - Basic usage with CompletableFuture
 * - Using callbacks for streaming responses
 * - Handling Outcome types (Success/Error/Timeout)
 * - Using ClaudeCodeRequest builder
 * - Error handling patterns
 * - Resource management with try-with-resources
 * <p>
 * Note: When using callbacks from Java, you need to work with Kotlin's Function1 type
 * and return Unit.INSTANCE. This is because the SDK is written in Kotlin.
 * See Example 3 for both verbose and lambda approaches.
 */
public class JavaExample {

    public static void main(String[] args) {
        System.out.println("Claude Code SDK Java Example");
        System.out.println("============================\n");

        // Example 1: Basic usage with try-with-resources
        example1BasicUsage();

        // Example 2: Using CompletableFuture and async operations
        example2CompletableFuture();

        // Example 3: Using callbacks for streaming responses
        example3Callbacks();

        // Example 4: Using ClaudeCodeRequest builder
        example4RequestBuilder();

        // Example 5: Handling Outcome types
        example5OutcomeHandling();

        // Example 6: Advanced configuration
        example6AdvancedConfiguration();

        // Example 7: Error handling patterns
        example7ErrorHandling();

        System.out.println("\nAll examples completed!");
    }

    /**
     * Example 1: Basic usage with try-with-resources.
     * <p>
     * Shows the simplest way to use the SDK with automatic resource management.
     */
    private static void example1BasicUsage() {
        System.out.println("Example 1: Basic usage with try-with-resources");
        System.out.println("----------------------------------------------");

        // Create client using try-with-resources for automatic cleanup
        try (ClaudeCodeClient client = ClaudeCode.createClient()) {

            // Send a simple query and wait for all messages
            CompletableFuture<List<Message>> future = client.queryAsync("What is 2 + 2?");

            // Wait for the response (blocking)
            List<Message> messages = future.get();

            // Process messages
            for (Message message : messages) {
                if (message instanceof AssistantMessage) {
                    AssistantMessage assistant = (AssistantMessage) message;
                    System.out.println("Assistant: " + assistant.getText());
                } else if (message instanceof ResultMessage) {
                    ResultMessage result = (ResultMessage) message;
                    System.out.println("Completed in " + result.getDurationMs() + "ms");
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * Example 2: Using CompletableFuture and async operations.
     * <p>
     * Shows how to work with async operations without blocking.
     */
    private static void example2CompletableFuture() {
        System.out.println("Example 2: CompletableFuture and async operations");
        System.out.println("-------------------------------------------------");

        try (ClaudeCodeClient client = ClaudeCode.createClient()) {

            // Send query and process response asynchronously
            client.queryAsync("Tell me a fun fact about programming")
                    .thenAccept(messages -> {
                        System.out.println("Received " + messages.size() + " messages");

                        // Process each message
                        messages.forEach(message -> {
                            if (message instanceof AssistantMessage) {
                                System.out.println("Fun fact: " + ((AssistantMessage) message).getText());
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        System.err.println("Async error: " + throwable.getMessage());
                        return null;
                    })
                    .join(); // Wait for completion

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * Example 3: Using callbacks for streaming responses.
     * <p>
     * Shows how to process messages as they arrive using callbacks.
     */
    private static void example3Callbacks() {
        System.out.println("Example 3: Using callbacks for streaming responses");
        System.out.println("--------------------------------------------------");

        try (ClaudeCodeClient client = ClaudeCode.createClient()) {

            // Use callback to process messages as they arrive
            // Note: queryWithCallback blocks until all messages are received
            Function1<Message, Unit> messageCallback = new Function1<Message, Unit>() {
                @Override
                public Unit invoke(Message message) {
                    // Process each message as it arrives
                    if (message instanceof SystemMessage) {
                        SystemMessage system = (SystemMessage) message;
                        System.out.println("[System] " + system.getSubtype());
                    } else if (message instanceof AssistantMessage) {
                        AssistantMessage assistant = (AssistantMessage) message;
                        // Print text content without newline for streaming effect
                        System.out.print(assistant.getText());
                    } else if (message instanceof ResultMessage) {
                        ResultMessage result = (ResultMessage) message;
                        System.out.println("\n[Completed] Duration: " + result.getDurationMs() + "ms");
                        if (result.getTotalCostUsd() != null) {
                            System.out.println("[Cost] $" + result.getTotalCostUsd());
                        }
                    }
                    return Unit.INSTANCE;
                }
            };

            client.queryWithCallback(
                    "Write a haiku about coding",
                    ClaudeCodeOptions.builder().build(),
                    messageCallback
            );

            // Alternative: Using lambda expression (more concise)
            System.out.println("\nAlternative approach with lambda:");
            client.queryWithCallback(
                    "What is the meaning of life?",
                    ClaudeCodeOptions.builder().build(),
                    message -> {
                        if (message instanceof AssistantMessage) {
                            System.out.print(((AssistantMessage) message).getText());
                        }
                        return Unit.INSTANCE;
                    }
            );

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * Example 4: Using ClaudeCodeRequest builder.
     * <p>
     * Shows how to build complex requests with various options.
     */
    private static void example4RequestBuilder() {
        System.out.println("Example 4: Using ClaudeCodeRequest builder");
        System.out.println("------------------------------------------");

        try (ClaudeCodeClient client = ClaudeCode.createClient()) {

            // Build a request with various options
            ClaudeCodeRequest request = ClaudeCodeRequest.builder()
                    .prompt("List the files in the current directory")
                    .allowedTools(Arrays.asList("read", "write", "bash"))
                    .systemPrompt("You are a helpful file system assistant")
                    .maxThinkingTokens(4000)
                    .maxTurns(3)
                    .timeoutMs(60000) // 1 minute timeout
                    .build();

            // Send the request
            CompletableFuture<List<Message>> future = client.queryAsync(request);

            // Process the response
            List<Message> messages = future.get(70, TimeUnit.SECONDS);

            for (Message message : messages) {
                if (message instanceof AssistantMessage) {
                    AssistantMessage assistant = (AssistantMessage) message;

                    // Check for tool use
                    if (!assistant.getToolUseBlocks().isEmpty()) {
                        System.out.println("Assistant is using tools:");
                        for (ToolUseBlock toolUse : assistant.getToolUseBlocks()) {
                            System.out.println("  - Tool: " + toolUse.getName());
                            System.out.println("    Input: " + toolUse.getInput());
                        }
                    }

                    // Print text content
                    if (!assistant.getText().isEmpty()) {
                        System.out.println("Assistant: " + assistant.getText());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * Example 5: Handling Outcome types.
     * <p>
     * Shows how to handle Success/Error/Timeout outcomes elegantly.
     */
    private static void example5OutcomeHandling() {
        System.out.println("Example 5: Handling Outcome types");
        System.out.println("---------------------------------");

        try (ClaudeCodeClient client = ClaudeCode.createClient()) {

            // Query with outcome handling
            CompletableFuture<Outcome<List<Message>>> outcomeFuture =
                    client.queryOutcomeAsync("What's the weather like today?");

            // Wait for outcome
            Outcome<List<Message>> outcome = outcomeFuture.get();

            // Handle different outcome types
            if (outcome instanceof Outcome.Success) {
                Outcome.Success<List<Message>> success = (Outcome.Success<List<Message>>) outcome;
                System.out.println("Success! Got " + success.getValue().size() + " messages");
                System.out.println("Duration: " + success.getDurationMs() + "ms");

                // Process messages
                for (Message msg : success.getValue()) {
                    if (msg instanceof AssistantMessage) {
                        System.out.println("Response: " + ((AssistantMessage) msg).getText());
                    }
                }

            } else if (outcome instanceof Outcome.Error) {
                Outcome.Error error = (Outcome.Error) outcome;
                System.err.println("Error occurred: " + error.getException().getMessage());
                System.err.println("Error type: " + error.getException().getClass().getSimpleName());

                // Check partial messages
                if (!error.getPartialMessages().isEmpty()) {
                    System.out.println("Received " + error.getPartialMessages().size() +
                            " partial messages before error");
                }

            } else if (outcome instanceof Outcome.Timeout) {
                Outcome.Timeout timeout = (Outcome.Timeout) outcome;
                System.err.println("Request timed out after " + timeout.getDurationMs() + "ms");

                // Check partial messages
                if (!timeout.getPartialMessages().isEmpty()) {
                    System.out.println("Received " + timeout.getPartialMessages().size() +
                            " partial messages before timeout");
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * Example 6: Advanced configuration.
     * <p>
     * Shows how to use advanced options and configurations.
     */
    private static void example6AdvancedConfiguration() {
        System.out.println("Example 6: Advanced configuration");
        System.out.println("---------------------------------");

        try (ClaudeCodeClient client = ClaudeCode.createClient()) {

            // Create options with builder pattern
            ClaudeCodeOptions options = ClaudeCodeOptions.builder()
                    .allowedTools(Arrays.asList("read", "write", "bash", "edit"))
                    .disallowedTools(Arrays.asList("rm", "delete"))
                    .systemPrompt("You are a Python expert. Always write clean, idiomatic Python code.")
                    .appendSystemPrompt("Follow PEP 8 style guidelines.")
                    .maxThinkingTokens(10000)
                    .model("claude-3-sonnet-20240229")
                    .cwd(Paths.get("/tmp"))
                    .permissionMode(PermissionMode.ACCEPT_EDITS)
                    .timeoutMs(120000) // 2 minutes
                    .build();

            // Create request with options
            ClaudeCodeRequest request = ClaudeCodeRequest.builder()
                    .prompt("Create a simple Python script that reads a CSV file and prints summary statistics")
                    .build();

            // Send request with custom options
            client.queryAsync(request.getPrompt(), options)
                    .thenCompose(messages -> {
                        System.out.println("Received response with " + messages.size() + " messages");

                        // Chain another request based on the response
                        boolean hasCode = messages.stream()
                                .filter(m -> m instanceof AssistantMessage)
                                .map(m -> (AssistantMessage) m)
                                .anyMatch(m -> m.getText().contains("```python"));

                        if (hasCode) {
                            System.out.println("Code was generated! Asking for improvements...");
                            return client.queryAsync("Can you add error handling to the script?", options);
                        } else {
                            return CompletableFuture.completedFuture(messages);
                        }
                    })
                    .thenAccept(finalMessages -> {
                        // Process final messages
                        for (Message msg : finalMessages) {
                            if (msg instanceof AssistantMessage) {
                                System.out.println("Final response: " + ((AssistantMessage) msg).getText());
                            }
                        }
                    })
                    .join();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * Example 7: Error handling patterns.
     * <p>
     * Shows various error handling strategies.
     */
    private static void example7ErrorHandling() {
        System.out.println("Example 7: Error handling patterns");
        System.out.println("----------------------------------");

        // Pattern 1: Try-catch with specific exception types
        try (ClaudeCodeClient client = ClaudeCode.createClient()) {

            // Create a request that might fail
            ClaudeCodeRequest request = ClaudeCodeRequest.builder()
                    .prompt("Execute a potentially dangerous command")
                    .allowedTools(Arrays.asList("nonexistent_tool"))
                    .timeoutMs(5000) // Short timeout
                    .build();

            // Pattern 1: Handle specific exceptions
            try {
                List<Message> messages = client.queryAsync(request).get(10, TimeUnit.SECONDS);
                System.out.println("Got " + messages.size() + " messages");
            } catch (ExecutionException e) {
                // Unwrap the actual exception
                Throwable cause = e.getCause();
                if (cause instanceof ClaudeSDKException) {
                    System.err.println("Claude SDK error: " + cause.getMessage());
                } else {
                    System.err.println("Unexpected error: " + cause.getMessage());
                }
            } catch (InterruptedException e) {
                System.err.println("Operation was interrupted");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("General error: " + e.getMessage());
            }

            // Pattern 2: Using outcome for cleaner error handling
            CompletableFuture<Outcome<List<Message>>> outcomeFuture =
                    client.queryOutcomeAsync("Tell me about error handling in Java");

            outcomeFuture
                    .thenAccept(outcome -> {
                        // Chain handlers for each outcome type
                        if (outcome instanceof Outcome.Success) {
                            handleSuccess((Outcome.Success<List<Message>>) outcome);
                        } else if (outcome instanceof Outcome.Error) {
                            handleError((Outcome.Error) outcome);
                        } else if (outcome instanceof Outcome.Timeout) {
                            handleTimeout((Outcome.Timeout) outcome);
                        }
                    })
                    .exceptionally(throwable -> {
                        System.err.println("Failed to get outcome: " + throwable.getMessage());
                        return null;
                    })
                    .join();

            // Pattern 3: Async error recovery
            System.out.println("\nPattern 3: Async error recovery");

            CountDownLatch latch = new CountDownLatch(1);

            client.queryAsync("This might fail")
                    .handle((messages, throwable) -> {
                        if (throwable != null) {
                            System.err.println("Query failed: " + throwable.getMessage());
                            // Return a default response
                            return Arrays.asList(new UserMessage("Query failed, using fallback"));
                        }
                        return messages;
                    })
                    .thenAccept(messages -> {
                        System.out.println("Processed " + messages.size() + " messages (possibly fallback)");
                        latch.countDown();
                    });

            // Wait for async operation
            try {
                latch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } catch (Exception e) {
            System.err.println("Client creation error: " + e.getMessage());
        }

        System.out.println();
    }

    // Helper methods for outcome handling

    private static void handleSuccess(Outcome.Success<List<Message>> success) {
        System.out.println("Request succeeded!");
        System.out.println("Duration: " + success.getDurationMs() + "ms");
        System.out.println("Messages: " + success.getValue().size());

        // Extract and print assistant responses
        success.getValue().stream()
                .filter(m -> m instanceof AssistantMessage)
                .map(m -> (AssistantMessage) m)
                .forEach(assistant -> {
                    if (!assistant.getText().isEmpty()) {
                        System.out.println("Assistant says: " + assistant.getText());
                    }
                });
    }

    private static void handleError(Outcome.Error error) {
        System.err.println("Request failed with error!");
        System.err.println("Exception: " + error.getException().getClass().getSimpleName());
        System.err.println("Message: " + error.getException().getMessage());
        System.err.println("Duration until error: " + error.getDurationMs() + "ms");

        if (!error.getPartialMessages().isEmpty()) {
            System.out.println("Partial messages received: " + error.getPartialMessages().size());
        }
    }

    private static void handleTimeout(Outcome.Timeout timeout) {
        System.err.println("Request timed out!");
        System.err.println("Timeout after: " + timeout.getDurationMs() + "ms");

        if (!timeout.getPartialMessages().isEmpty()) {
            System.out.println("Partial messages before timeout: " + timeout.getPartialMessages().size());
            // You could process partial messages here
        }
    }
}