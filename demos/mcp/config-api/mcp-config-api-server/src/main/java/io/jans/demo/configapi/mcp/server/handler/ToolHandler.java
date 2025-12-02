package io.jans.demo.configapi.mcp.server.handler;

import io.jans.demo.configapi.mcp.server.service.JansConfigApiClient;
import io.jans.demo.configapi.mcp.server.model.OidcClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import java.util.List;
import java.util.Map;

public class ToolHandler {

    private final JansConfigApiClient apiClient;
    private final ObjectMapper objectMapper;

    public ToolHandler(JansConfigApiClient apiClient) {
        this.apiClient = apiClient;
        this.objectMapper = new ObjectMapper();
    }

    public CallToolResult handleListClients(Map<String, Object> arguments) {
        try {
            // Extract query parameters from arguments
            // Handle both String and Number types (different LLMs send different types)
            Integer limit = 50;
            if (arguments.get("limit") != null) {
                Object limitObj = arguments.get("limit");
                try {
                    limit = limitObj instanceof Number
                            ? ((Number) limitObj).intValue()
                            : Integer.parseInt(limitObj.toString());
                    limit = Math.max(1, Math.min(limit, 200)); // Clamp to valid range
                } catch (NumberFormatException e) {
                    limit = 50; // Fall back to default
                }
            }

            Integer startIndex = 0;
            if (arguments.get("startIndex") != null) {
                Object startIndexObj = arguments.get("startIndex");
                startIndex = startIndexObj instanceof Number
                        ? ((Number) startIndexObj).intValue()
                        : Integer.parseInt(startIndexObj.toString());
            }

            String sortBy = arguments.get("sortBy") != null
                    ? arguments.get("sortBy").toString()
                    : "inum";

            String sortOrder = arguments.get("sortOrder") != null
                    ? arguments.get("sortOrder").toString()
                    : "ascending";

            // Call API with parameters
            List<OidcClient> clients = apiClient.getAllClients(limit, startIndex, sortBy, sortOrder);
            String jsonResult = objectMapper.writeValueAsString(clients);

            return CallToolResult.builder()
                    .content(List.of(new TextContent(jsonResult)))
                    .isError(false)
                    .build();
        } catch (Exception e) {
            e.printStackTrace(); // Print full stack trace for debugging
            return createErrorResult("Error listing OIDC clients: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private CallToolResult createErrorResult(String errorMessage) {
        return CallToolResult.builder()
                .content(List.of(new TextContent(errorMessage)))
                .isError(true)
                .build();
    }
}
