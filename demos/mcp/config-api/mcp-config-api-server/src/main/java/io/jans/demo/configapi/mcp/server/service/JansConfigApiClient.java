package io.jans.demo.configapi.mcp.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

public class JansConfigApiClient {

    private static final Logger logger = LoggerFactory.getLogger(JansConfigApiClient.class);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final String accessToken;
    private final boolean devMode;

    public JansConfigApiClient(String baseUrl, String accessToken, boolean devMode) {
        this.baseUrl = baseUrl;
        this.accessToken = accessToken;
        this.devMode = devMode;
        this.objectMapper = new ObjectMapper();
        this.httpClient = createHttpClient();
    }

    /**
     * Creates an HttpClient with optional SSL certificate bypass for development
     */
    private HttpClient createHttpClient() {
        try {
            HttpClient.Builder builder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10));

            if (devMode) {
                // DEVELOPMENT MODE: Trust all SSL certificates (for self-signed certs)
                logger.warn("WARNING: Running in DEV mode - SSL certificate validation is DISABLED");

                TrustManager[] trustAllCerts = new TrustManager[] {
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }

                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }

                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };

                SSLContext sslContext;
                try {
                    sslContext = SSLContext.getInstance("TLSv1.3");
                } catch (Exception e) {
                    logger.warn("TLSv1.3 not available, falling back to TLSv1.2");
                    sslContext = SSLContext.getInstance("TLSv1.2");
                }
                sslContext.init(null, trustAllCerts, new SecureRandom());

                // Explicitly restrict to modern TLS protocols
                javax.net.ssl.SSLParameters sslParams = new javax.net.ssl.SSLParameters();
                sslParams.setProtocols(new String[] { "TLSv1.3", "TLSv1.2" });

                builder.sslContext(sslContext)
                        .sslParameters(sslParams);
            } else {
                // PRODUCTION MODE: Use default SSL certificate validation
                logger.info("Running in PRODUCTION mode - SSL certificate validation is ENABLED");
            }

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create HTTP client", e);
        }
    }

    /**
     * Gets all OpenID Connect clients from the Jans Config API
     * 
     * @param limit      Maximum number of clients to return (default: 50)
     * @param startIndex Zero-based index of first result (default: 0)
     * @param sortBy     Field to sort by (default: "inum")
     * @param sortOrder  Sort order: "ascending" or "descending" (default:
     *                   "ascending")
     * @return List of JsonNode objects representing OIDC clients
     * @throws Exception if API call fails
     */
    public List<JsonNode> getAllClients(Integer limit, Integer startIndex, String sortBy, String sortOrder)
            throws IOException, InterruptedException {

        // Set default values
        if (limit == null)
            limit = 50;
        if (startIndex == null)
            startIndex = 0;
        if (sortBy == null || sortBy.isEmpty())
            sortBy = "inum";
        if (sortOrder == null || sortOrder.isEmpty())
            sortOrder = "ascending";

        // Build URL with query parameters
        String url = String.format(
                "%s/jans-config-api/api/v1/openid/clients?limit=%d&startIndex=%d&sortBy=%s&sortOrder=%s",
                baseUrl,
                limit,
                startIndex,
                encodeValue(sortBy),
                encodeValue(sortOrder));

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(30));

        // Add OAuth2 Bearer token
        if (accessToken != null && !accessToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + accessToken);
        } else {
            logger.warn("No access token provided - API calls may fail with 401 Unauthorized");
        }

        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("Jans Config API Error: " + response.statusCode() + " - " + response.body());
        }

        // The API returns a PagedResult, but we need to extract the "entries" array
        // For now, we'll try to parse directly as a list first, and if that fails,
        // parse as PagedResult
        try {
            // Try direct list parsing
            return objectMapper.readValue(
                    response.body(),
                    new TypeReference<List<JsonNode>>() {
                    });
        } catch (Exception e) {
            // If that fails, try parsing as PagedResult and extract entries
            var jsonNode = objectMapper.readTree(response.body());
            if (jsonNode.has("entries")) {
                return objectMapper.readValue(
                        jsonNode.get("entries").toString(),
                        new TypeReference<List<JsonNode>>() {
                        });
            }
            throw e;
        }
    }

    /**
     * Creates a new OpenID Connect client in the Jans Config API
     * 
     * @param clientPayload JSON payload representing the client to create
     * @return JsonNode representing the created OIDC client
     * @throws Exception if API call fails
     */
    public JsonNode createClient(JsonNode clientPayload) throws IOException, InterruptedException {
        String url = String.format("%s/jans-config-api/api/v1/openid/clients", baseUrl);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(clientPayload.toString()))
                .timeout(Duration.ofSeconds(30));

        // Add OAuth2 Bearer token
        if (accessToken != null && !accessToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + accessToken);
        } else {
            logger.warn("No access token provided - API calls may fail with 401 Unauthorized");
        }

        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("Jans Config API Error: " + response.statusCode() + " - " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    /**
     * Gets application health status from the Jans Config API
     * 
     * @return JsonNode representing the health status
     * @throws Exception if API call fails
     */
    public JsonNode getHealth() throws IOException, InterruptedException {
        String url = String.format("%s/jans-config-api/api/v1/health", baseUrl);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(30));

        // Add OAuth2 Bearer token if available
        if (accessToken != null && !accessToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + accessToken);
        }

        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("Jans Config API Error: " + response.statusCode() + " - " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    /**
     * URL encode a value for use in query parameters
     */
    private String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
