package io.jans.demo.configapi.mcp.server.handler;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ListPromptsResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import java.util.List;
import java.util.Map;

public class PromptHandler {

    public ListPromptsResult listPrompts(String cursor) {
        return new ListPromptsResult(
                List.of(
                        new Prompt(
                                "analyze-resource",
                                "Analyze a resource by ID",
                                List.of(
                                        new PromptArgument("resourceId", "The ID of the resource to analyze", true)))),
                null);
    }

    public GetPromptResult getPrompt(String name, Map<String, Object> arguments) {
        if ("analyze-resource".equals(name)) {
            String resourceId = (String) arguments.get("resourceId");
            if (resourceId == null) {
                throw new IllegalArgumentException("resourceId argument is required");
            }

            return new GetPromptResult(
                    "analyze-resource",
                    List.of(
                            new PromptMessage(
                                    Role.USER,
                                    new TextContent("Please analyze the following resource:\n\n" +
                                            "Resource ID: " + resourceId + "\n\n" +
                                            "Use the 'get_resource_by_id' tool to fetch details about this resource and then provide a summary."))));
        }

        throw new IllegalArgumentException("Unknown prompt: " + name);
    }
}
