package io.jans.demo.configapi.mcp.server;

import io.jans.demo.configapi.mcp.server.handler.ToolHandler;
import io.jans.demo.configapi.mcp.server.service.AuthorizationService;
import io.jans.demo.configapi.mcp.server.service.JansConfigApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class McpServerMain {

        private static final Logger logger = LoggerFactory.getLogger(McpServerMain.class);

        public static void main(String[] args) {
                Server jettyServer = null;
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
                        String baseUrl = System.getenv("JANS_HOST_URL");
                        String accessToken = System.getenv("JANS_OAUTH_ACCESS_TOKEN");
                        int serverPort = Integer.parseInt(
                                        System.getenv().getOrDefault("MCP_SERVER_PORT", "8080"));

                        if (baseUrl == null || baseUrl.isEmpty()) {
                                throw new IllegalStateException("JANS_HOST_URL environment variable is required");
                        }
                        if (accessToken == null || accessToken.isEmpty()) {
                                throw new IllegalStateException(
                                                "JANS_OAUTH_ACCESS_TOKEN environment variable is required");
                        }

                        // Create API client, authorization service, and tool handler
                        JansConfigApiClient apiClient = new JansConfigApiClient(baseUrl, accessToken, devMode);
                        AuthorizationService authorizationService = AuthorizationService.initAuthorizationService();

                        ToolHandler toolHandler = new ToolHandler(apiClient, authorizationService);

                        // Create JSON mapper
                        ObjectMapper objectMapper = new ObjectMapper();
                        JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);

                        // Create HTTP/Streamable transport provider (replaces deprecated SSE)
                        HttpServletStreamableServerTransportProvider transportProvider = HttpServletStreamableServerTransportProvider
                                        .builder()
                                        .mcpEndpoint("/mcp")
                                        .jsonMapper(jsonMapper)
                                        .build();

                        // Create server capabilities
                        McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
                                        .logging()
                                        .prompts(true)
                                        .resources(true, false)
                                        .tools(true)
                                        .build();

                        // Create and configure MCP server
                        McpSyncServer mcpServer = McpServer.sync(transportProvider)
                                        .serverInfo("jans-config-api-server", "2.0.0")
                                        .capabilities(capabilities)
                                        .build();

                        // Register tools
                        registerTools(mcpServer, toolHandler);

                        // Create and configure Jetty server
                        jettyServer = new Server(serverPort);
                        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
                        context.setContextPath("/");

                        // Register the MCP servlet (handles StreamableHttp POST messages)
                        ServletHolder mcpServletHolder = new ServletHolder(transportProvider);
                        context.addServlet(mcpServletHolder, "/mcp/*");

                        jettyServer.setHandler(context);

                        // Start Jetty server
                        jettyServer.start();

                        logger.info("=================================================");
                        logger.info("MCP Jans Config API Server started successfully");
                        logger.info("Server Port: {}", serverPort);
                        logger.info("MCP Endpoint: http://localhost:{}/mcp", serverPort);
                        logger.info("Connect with: npx @modelcontextprotocol/inspector http://localhost:{}/mcp",
                                        serverPort);
                        logger.info("=================================================");

                        // Keep server running
                        final Server finalJettyServer = jettyServer;
                        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                                logger.info("Shutting down MCP server...");
                                try {
                                        mcpServer.close();
                                        finalJettyServer.stop();
                                } catch (Exception e) {
                                        logger.error("Error during shutdown", e);
                                }
                        }));

                        // Block main thread
                        jettyServer.join();

                } catch (Exception e) {
                        logger.error("Failed to start MCP server", e);
                        if (jettyServer != null) {
                                try {
                                        jettyServer.stop();
                                } catch (Exception stopException) {
                                        logger.error("Error stopping server", stopException);
                                }
                        }
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

                // Tool: create_client
                server.addTool(createTool(
                                "create_client",
                                "Creates a new OpenID Connect client in the Jans Config API. This tool accepts a JSON object representing the client configuration.",
                                Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                                "client_data", Map.of(
                                                                                "type", "object",
                                                                                "description",
                                                                                "JSON object containing the OIDC client configuration. Must include fields like redirectUris, responseTypes, grantTypes, applicationType, etc.")),
                                                "required", List.of("client_data")),
                                toolHandler::handleCreateClient));

                // Tool: get_health
                server.addTool(createTool(
                                "get_health",
                                "Returns application health status from the Jans Config API",
                                Map.of(
                                                "type", "object",
                                                "properties", Map.of(),
                                                "required", List.of()),
                                toolHandler::handleGetHealth));
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

        @FunctionalInterface
        interface ToolExecutor {
                CallToolResult execute(Map<String, Object> arguments);
        }
}
