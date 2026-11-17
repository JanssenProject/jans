/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.lock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/** Calls the Auth Server token endpoint and the Lock TRACE API. TLS certificates are always verified. */
public class LockClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final LockSettings config;
    private final HttpClient http;

    public LockClient(LockSettings config) {
        this.config = config;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** @return an access token for the client with all three trace scopes */
    public String token() throws IOException, InterruptedException {
        String scope = String.join(" ", LockSettings.SCOPE_ADMIN, LockSettings.SCOPE_WRITE,
                LockSettings.SCOPE_READONLY);
        String body = "grant_type=client_credentials&scope=" + enc(scope);
        String basicAuth = Base64.getEncoder().encodeToString(
                (config.clientId() + ":" + config.clientSecret()).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.tokenEndpoint()))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Basic " + basicAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("token request to " + config.tokenEndpoint() + " failed: HTTP "
                    + response.statusCode() + ". Check the client id, the client secret and that the client holds"
                    + " the trace.admin, trace.write and trace.readonly scopes");
        }
        String token = MAPPER.readTree(response.body()).path("access_token").asText(null);
        if (token == null) {
            throw new IllegalStateException("token response from " + config.tokenEndpoint() + " has no access_token");
        }
        return token;
    }

    public void registerProducerKey(String producerId, String kid, TraceSigningKey key, String adminToken)
            throws IOException, InterruptedException {
        var body = MAPPER.createObjectNode();
        body.put("producer_id", producerId);
        body.put("kid", kid);
        var jwk = body.putObject("public_key_jwk");
        key.publicJwk().forEach(jwk::put);
        body.putArray("authorized_event_kinds").add("AUTHORIZATION_DECISION");
        body.put("valid_from", "2020-01-01T00:00:00Z");
        body.putNull("valid_until");
        expectCreated("key registration", post("/audit/trace/admin/producer-keys", body.toString(), adminToken));
    }

    public void registerProducerChain(String producerId, String instanceId, String chainId, String adminToken)
            throws IOException, InterruptedException {
        var body = MAPPER.createObjectNode();
        body.put("producer_id", producerId);
        body.put("producer_instance_id", instanceId);
        body.put("producer_chain_id", chainId);
        expectCreated("chain registration", post("/audit/trace/admin/producer-chains", body.toString(), adminToken));
    }

    /**
     * Fetches the stored TRACE records of one execution from Lock.
     * <p>
     * Polls every 2 seconds until at least {@code expected} records are returned or {@code timeout} passes.
     *
     * @return the last response body. It can hold fewer than {@code expected} records after a timeout. It is an empty
     *         object if Lock never answered with 200.
     */
    public JsonNode fetchExecution(String executionId, String authority, String readToken, int expected,
            Duration timeout) throws IOException, InterruptedException {
        String url = config.apiBase() + "/audit/trace/executions/" + enc(executionId)
                + "?execution_authority=" + enc(authority) + "&start=0&count=1000";
        long deadline = System.nanoTime() + timeout.toNanos();
        JsonNode last = MAPPER.createObjectNode();
        while (true) {
            HttpResponse<String> response = get(url, readToken);
            if (response.statusCode() == 200) {
                last = MAPPER.readTree(response.body());
                if (last.path("records").size() >= expected) {
                    return last;
                }
            }
            if (System.nanoTime() >= deadline) {
                return last;
            }
            Thread.sleep(2000);
        }
    }

    private HttpResponse<String> post(String path, String json, String token)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.apiBase() + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String url, String token) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void expectCreated(String what, HttpResponse<String> response) {
        if (response.statusCode() != 201) {
            throw new IllegalStateException(what + " failed: HTTP " + response.statusCode() + " " + response.body());
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
