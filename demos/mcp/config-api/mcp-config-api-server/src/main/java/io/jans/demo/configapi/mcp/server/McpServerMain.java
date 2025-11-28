package io.jans.demo.configapi.mcp.server;

import io.jans.demo.configapi.mcp.server.handler.ToolHandler;
import io.jans.demo.configapi.mcp.server.handler.ResourceHandler;
import io.jans.demo.configapi.mcp.server.handler.PromptHandler;
import io.jans.demo.configapi.mcp.server.service.JansConfigApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class McpServerMain {

        private static final Logger logger = LoggerFactory.getLogger(McpServerMain.class);

        public static void main(String[] args) {
                try {
                        // Parse command-line arguments
                        boolean devMode = false;
                        for (String arg : args) {
                                if ("-dev".equals(arg)) {
                                        devMode = true;
                                        break;
                                }
                        }

                        // Load configuration from environment variables
                        String baseUrl = System.getenv("JANS_CONFIG_API_URL");
                        String accessToken = System.getenv("JANS_OAUTH_ACCESS_TOKEN");

                        if (baseUrl == null || baseUrl.isEmpty()) {
                                throw new IllegalStateException("JANS_CONFIG_API_URL environment variable is required");
                        }
                        if (accessToken == null || accessToken.isEmpty()) {
                                throw new IllegalStateException(
                                                "JANS_OAUTH_ACCESS_TOKEN environment variable is required");
                        }

                        // Create API client and tool handler
                        JansConfigApiClient apiClient = new JansConfigApiClient(baseUrl, accessToken, devMode);
                        ToolHandler toolHandler = new ToolHandler(apiClient);
                        ResourceHandler resourceHandler = new ResourceHandler(apiClient);
                        PromptHandler promptHandler = new PromptHandler();

                        // Create JSON mapper
                        ObjectMapper objectMapper = new ObjectMapper();

                        // Create STDIO transport
                        StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(
                                        new JacksonMcpJsonMapper(objectMapper));

                        // Create server capabilities
                        McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
                                        .logging()
                                        .prompts(true)
                                        .resources(true, false)
                                        .tools(true)
                                        .build();

                        // Create and configure MCP server
                        McpSyncServer server = McpServer.sync(transportProvider)
                                        .serverInfo("jans-oidc-clients-viewer", "2.0.0")
                                        .capabilities(capabilities)
                                        .resourceTemplates(createResourceTemplate(resourceHandler))
                                        .prompts(createPrompt(promptHandler))
                                        .build();

                        // Register tools
                        registerTools(server, toolHandler);

                        logger.info("MCP Jans OIDC Clients Viewer started successfully");

                        // Keep server running
                        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                                logger.info("Shutting down MCP server...");
                                server.close();
                        }));

                        // Block main thread
                        Thread.currentThread().join();

                } catch (Exception e) {
                        logger.error("Failed to start MCP server", e);
                        System.exit(1);
                }
        }

        private static void registerTools(McpSyncServer server, ToolHandler toolHandler) {
                // Tool: list_clients
                server.addTool(createTool(
                                "list_clients",
                                "Lists OpenID Connect clients from the Jans Config API. USE THIS TOOL ONLY when the user explicitly asks to list, view, retrieve, or get information about OIDC clients. DO NOT use for greetings or general conversation.",
                                Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                                "limit", Map.of(
                                                                                "type", "integer",
                                                                                "description",
                                                                                "Maximum number of clients to return (default: 50, max: 200)",
                                                                                "default", 50),
                                                                "startIndex", Map.of(
                                                                                "type", "integer",
                                                                                "description",
                                                                                "Zero-based index of first result (default: 0)",
                                                                                "default", 0),
                                                                "sortBy", Map.of(
                                                                                "type", "string",
                                                                                "description",
                                                                                "Field to sort by (default: inum)",
                                                                                "default", "inum"),
                                                                "sortOrder", Map.of(
                                                                                "type", "string",
                                                                                "description",
                                                                                "Sort order: ascending or descending (default: ascending)",
                                                                                "enum",
                                                                                List.of("ascending", "descending"),
                                                                                "default", "ascending")),
                                                "required", List.of()),
                                toolHandler::handleListClients));
        }

        private static McpServerFeatures.SyncToolSpecification createTool(
                        String name,
                        String description,
                        Map<String, Object> inputSchema,
                        ToolExecutor executor) {
                // Convert Map to JsonSchema using ObjectMapper
                ObjectMapper mapper = new ObjectMapper();
                McpSchema.JsonSchema schema = mapper.convertValue(inputSchema, McpSchema.JsonSchema.class);

                Tool tool = Tool.builder()
                                .name(name)
                                .description(description)
                                .inputSchema(schema)
                                .build();

                return new McpServerFeatures.SyncToolSpecification(
                                tool,
                                (exchange, arguments) -> executor.execute(arguments));
        }

        private static McpServerFeatures.SyncResourceTemplateSpecification createResourceTemplate(
                        ResourceHandler handler) {
                return new McpServerFeatures.SyncResourceTemplateSpecification(
                                new McpSchema.ResourceTemplate(
                                                "oidc-client://{inum}",
                                                "OIDC Client by inum",
                                                "Access an OpenID Connect client by its inum",
                                                "application/json",
                                                null),
                                (exchange, request) -> handler.readResource(request.uri()));
        }

        private static McpServerFeatures.SyncPromptSpecification createPrompt(PromptHandler handler) {
                return new McpServerFeatures.SyncPromptSpecification(
                                new McpSchema.Prompt(
                                                "analyze-resource",
                                                "Analyze a resource",
                                                java.util.List.of(
                                                                new McpSchema.PromptArgument("resourceId",
                                                                                "The ID of the resource to analyze",
                                                                                true))),
                                (exchange, request) -> handler.getPrompt(request.name(), request.arguments()));
        }

        @FunctionalInterface
        interface ToolExecutor {
                CallToolResult execute(Map<String, Object> arguments);
        }
}
