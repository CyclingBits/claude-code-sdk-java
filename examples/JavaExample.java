import net.cyclingbits.claudecode.*;
import net.cyclingbits.claudecode.types.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JavaExample {
    public static void main(String[] args) throws Exception {
        // Create client
        ClaudeCodeClient client = ClaudeCode.createClient();
        
        // Example 1: Simple async query
        System.out.println("=== Example 1: Simple Async Query ===");
        CompletableFuture<List<Message>> future = client.queryAsync(
            "Write a hello world function in Java"
        );
        
        List<Message> messages = future.get();
        for (Message message : messages) {
            if (message instanceof AssistantMessage) {
                AssistantMessage assistant = (AssistantMessage) message;
                System.out.println("Assistant: " + assistant.getText());
            } else if (message instanceof ResultMessage) {
                ResultMessage result = (ResultMessage) message;
                System.out.println("Cost: $" + result.getTotalCostUsd());
            }
        }
        
        // Example 2: Using options builder
        System.out.println("\n=== Example 2: Using Options Builder ===");
        ClaudeCodeOptions options = ClaudeCodeOptions.builder()
            .allowedTools(Arrays.asList("read", "write"))
            .systemPrompt("You are a Java expert")
            .model("claude-3-5-sonnet-20241022")
            .cwd(Paths.get("."))
            .maxTurns(3)
            .build();
        
        client.queryAsync("Help me optimize this Java code", options)
            .thenAccept(msgs -> {
                msgs.forEach(msg -> {
                    System.out.println("Message type: " + msg.getClass().getSimpleName());
                });
            })
            .join();
        
        // Example 3: Using callback
        System.out.println("\n=== Example 3: Using Callback ===");
        client.queryWithCallback(
            "Explain Java streams",
            ClaudeCodeOptions.builder().maxThinkingTokens(10000).build(),
            message -> {
                if (message instanceof AssistantMessage) {
                    AssistantMessage assistant = (AssistantMessage) message;
                    
                    // Process different content blocks
                    for (ContentBlock block : assistant.getContent()) {
                        if (block instanceof TextBlock) {
                            TextBlock text = (TextBlock) block;
                            System.out.println("Text: " + text.getText());
                        } else if (block instanceof ToolUseBlock) {
                            ToolUseBlock tool = (ToolUseBlock) block;
                            System.out.println("Tool use: " + tool.getName());
                        }
                    }
                }
            }
        );
        
        // Example 4: Error handling
        System.out.println("\n=== Example 4: Error Handling ===");
        client.queryAsync("This might fail")
            .exceptionally(throwable -> {
                System.err.println("Error occurred: " + throwable.getMessage());
                return Arrays.asList(); // Return empty list on error
            })
            .thenAccept(msgs -> {
                System.out.println("Completed with " + msgs.size() + " messages");
            })
            .join();
            
        // Example 5: Chaining operations
        System.out.println("\n=== Example 5: Chaining Operations ===");
        client.queryAsync("What's the best Java IDE?")
            .thenApply(msgs -> {
                // Extract only assistant messages
                return msgs.stream()
                    .filter(msg -> msg instanceof AssistantMessage)
                    .map(msg -> (AssistantMessage) msg)
                    .toList();
            })
            .thenAccept(assistantMsgs -> {
                System.out.println("Found " + assistantMsgs.size() + " assistant messages");
                assistantMsgs.forEach(msg -> {
                    System.out.println("- " + msg.getText());
                });
            })
            .join();
    }
}