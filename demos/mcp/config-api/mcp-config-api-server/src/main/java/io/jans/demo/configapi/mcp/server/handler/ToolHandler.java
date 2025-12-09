package io.jans.demo.configapi.mcp.server.handler;

import io.jans.demo.configapi.mcp.server.model.Permission;
import io.jans.demo.configapi.mcp.server.service.AuthorizationService;
import io.jans.demo.configapi.mcp.server.service.AuthorizationService.UnauthorizedException;
import io.jans.demo.configapi.mcp.server.service.JansConfigApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolHandler {

    private static final Logger logger = LoggerFactory.getLogger(ToolHandler.class);
    private final JansConfigApiClient apiClient;
    private final AuthorizationService authorizationService;
    private final ObjectMapper objectMapper;

    public ToolHandler(JansConfigApiClient apiClient, AuthorizationService authorizationService) {
        this.apiClient = apiClient;
        this.authorizationService = authorizationService;
        this.objectMapper = new ObjectMapper();
    }

    public CallToolResult handleListClients(Map<String, Object> arguments) {
        try {
            // Authorization checkpoint: Check if user has permission to read clients
            if (!authorizationService.checkAuthorization()) {
                throw new UnauthorizedException("Unauthorized access!");
            }

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
                try {
                    startIndex = startIndexObj instanceof Number
                            ? ((Number) startIndexObj).intValue()
                            : Integer.parseInt(startIndexObj.toString());
                    startIndex = Math.max(0, startIndex); // Ensure non-negative
                } catch (NumberFormatException e) {
                    startIndex = 0; // Fall back to default
                }
            }

            String sortBy = arguments.get("sortBy") != null
                    ? arguments.get("sortBy").toString()
                    : "inum";

            String sortOrder = arguments.get("sortOrder") != null
                    ? arguments.get("sortOrder").toString()
                    : "ascending";

            // Call API with parameters
            List<JsonNode> clients = apiClient.getAllClients(limit, startIndex, sortBy, sortOrder);
            String jsonResult = objectMapper.writeValueAsString(clients);

            return CallToolResult.builder()
                    .content(List.of(new TextContent(jsonResult)))
                    .isError(false)
                    .build();
        } catch (UnauthorizedException e) {
            logger.warn("Authorization failed for list_clients: {}", e.getMessage());
            return createErrorResult("Authorization failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error listing OIDC clients", e);
            return createErrorResult("Error listing OIDC clients: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public CallToolResult handleCreateClient(Map<String, Object> arguments) {
        try {
            // Authorization checkpoint: Check if user has permission to read clients
            if (!authorizationService.checkAuthorization()) {
                throw new UnauthorizedException("Unauthorized access!");
            }

            // Extract client_data from arguments
            Object clientDataObj = arguments.get("client_data");
            if (clientDataObj == null) {
                return createErrorResult("Missing required parameter: client_data");
            }

            // Parse the client_data into JsonNode
            JsonNode clientPayload;
            if (clientDataObj instanceof String) {
                // If it's a JSON string, parse it
                clientPayload = objectMapper.readTree((String) clientDataObj);
            } else {
                // If it's already a Map/Object, convert it to JsonNode
                clientPayload = objectMapper.valueToTree(clientDataObj);
            }

            // Call API to create the client
            JsonNode createdClient = apiClient.createClient(clientPayload);
            String jsonResult = objectMapper.writeValueAsString(createdClient);

            return CallToolResult.builder()
                    .content(List.of(new TextContent(jsonResult)))
                    .isError(false)
                    .build();
        } catch (UnauthorizedException e) {
            logger.warn("Authorization failed for create_client: {}", e.getMessage());
            return createErrorResult("Authorization failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating OIDC client", e);
            return createErrorResult("Error creating OIDC client: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public CallToolResult handleGetHealth(Map<String, Object> arguments) {
        try {
            // Authorization checkpoint: Check if user has permission to read clients
            if (!authorizationService.checkAuthorization()) {
                throw new UnauthorizedException("Unauthorized access!");
            }

            JsonNode healthStatus = apiClient.getHealth();
            String jsonResult = objectMapper.writeValueAsString(healthStatus);

            return CallToolResult.builder()
                    .content(List.of(new TextContent(jsonResult)))
                    .isError(false)
                    .build();
        } catch (UnauthorizedException e) {
            logger.warn("Authorization failed for get_health: {}", e.getMessage());
            return createErrorResult("Authorization failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error getting health status", e);
            return createErrorResult("Error getting health status: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private CallToolResult createErrorResult(String errorMessage) {
        return CallToolResult.builder()
                .content(List.of(new TextContent(errorMessage)))
                .isError(true)
                .build();
    }
}
