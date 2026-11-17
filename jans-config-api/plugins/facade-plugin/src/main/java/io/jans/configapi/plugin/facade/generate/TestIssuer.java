/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.generate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Local key server and RS256 token signer. The generated tokens are trusted only by the generated policy store. */
public class TestIssuer implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();

    private final KeyPair keyPair;
    private final String kid = "test-issuer-" + UUID.randomUUID().toString().substring(0, 8);
    private final HttpServer server;
    private final String issuer;
    private final String jwksJson;

    public TestIssuer() throws IOException, GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        this.keyPair = generator.generateKeyPair();
        this.jwksJson = jwks();
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        this.issuer = "http://localhost:" + server.getAddress().getPort();
        server.createContext("/jwks", exchange -> {
            byte[] body = jwksJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    public String issuer() {
        return issuer;
    }

    public String jwksUrl() {
        return issuer + "/jwks";
    }

    public String accessToken(String sub, String clientId) throws IOException, GeneralSecurityException {
        ObjectNode claims = baseClaims(sub, clientId);
        claims.put("client_id", clientId);
        claims.put("token_type", "Bearer");
        claims.putArray("scope").add("openid").add("profile");
        return sign(claims);
    }

    public String idToken(String sub, String clientId) throws IOException, GeneralSecurityException {
        ObjectNode claims = baseClaims(sub, clientId);
        claims.putArray("aud").add(clientId);
        return sign(claims);
    }

    public String userinfoToken(String sub, String clientId, String givenName, String inum, List<String> roles)
            throws IOException, GeneralSecurityException {
        ObjectNode claims = baseClaims(sub, clientId);
        claims.put("given_name", givenName);
        claims.put("inum", inum);
        var array = claims.putArray("role");
        roles.forEach(array::add);
        return sign(claims);
    }

    private ObjectNode baseClaims(String sub, String audience) {
        long now = Instant.now().getEpochSecond();
        ObjectNode claims = MAPPER.createObjectNode();
        claims.put("iss", issuer);
        claims.put("sub", sub);
        claims.put("aud", audience);
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("iat", now);
        claims.put("nbf", now - 5);
        claims.put("exp", now + 3600);
        return claims;
    }

    private String sign(ObjectNode claims) throws IOException, GeneralSecurityException {
        ObjectNode header = MAPPER.createObjectNode();
        header.put("alg", "RS256");
        header.put("typ", "JWT");
        header.put("kid", kid);
        String signingInput = B64URL.encodeToString(MAPPER.writeValueAsBytes(header)) + "."
                + B64URL.encodeToString(MAPPER.writeValueAsBytes(claims));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + B64URL.encodeToString(signature.sign());
    }

    private String jwks() throws IOException {
        RSAPublicKey key = (RSAPublicKey) keyPair.getPublic();
        ObjectNode jwk = MAPPER.createObjectNode();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", kid);
        jwk.put("n", B64URL.encodeToString(unsigned(key.getModulus())));
        jwk.put("e", B64URL.encodeToString(unsigned(key.getPublicExponent())));
        ObjectNode set = MAPPER.createObjectNode();
        set.putArray("keys").add(jwk);
        return MAPPER.writeValueAsString(set);
    }

    private static byte[] unsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return bytes;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
