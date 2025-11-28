package io.jans.demo.configapi.mcp.server.handler;

import io.jans.demo.configapi.mcp.server.service.JansConfigApiClient;
import io.jans.demo.configapi.mcp.server.model.OidcClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.ListResourcesResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceHandler {

    private final JansConfigApiClient apiClient;
    private final ObjectMapper objectMapper;

    public ResourceHandler(JansConfigApiClient apiClient) {
        this.apiClient = apiClient;
        this.objectMapper = new ObjectMapper();
    }

    public ListResourcesResult listResources(String cursor) {
        try {
            List<OidcClient> clients = apiClient.getAllClients(50, 0, "inum", "ascending");

            List<Resource> mcpResources = clients.stream()
                    .map(client -> Resource.builder()
                            .uri("oidc-client://" + client.getInum())
                            .name(client.getClientName() != null ? client.getClientName() : client.getDisplayName())
                            .description(client.getDescription() != null ? client.getDescription()
                                    : client.getApplicationType())
                            .mimeType("application/json")
                            .build())
                    .collect(Collectors.toList());

            return new ListResourcesResult(mcpResources, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to list resources", e);
        }
    }

    public ReadResourceResult readResource(String uri) {
        try {
            if (!uri.startsWith("oidc-client://")) {
                throw new IllegalArgumentException("Invalid resource URI: " + uri);
            }

            String inum = uri.substring("oidc-client://".length());

            List<OidcClient> clients = apiClient.getAllClients(200, 0, "inum", "ascending");
            OidcClient client = clients.stream()
                    .filter(c -> c.getInum().equals(inum))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("OIDC client not found: " + inum));

            String jsonContent = objectMapper.writeValueAsString(client);

            return new ReadResourceResult(
                    List.of(new TextResourceContents(
                            uri,
                            "application/json",
                            jsonContent)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read resource: " + uri, e);
        }
    }
}
