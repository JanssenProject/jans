/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.generate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds the policy store archive (.cjar) that Cedarling loads. The policies come from the {@code policy-store-acme}
 * bundled in the plugin jar. {@code trusted-issuers.json} is generated so that it trusts only the local test issuer.
 */
public final class AcmeStore {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AcmeStore.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String RESOURCE_ROOT = "/facade/policy-store-acme/";
    private static final String INDEX = "/facade/policy-store-acme.index";
    private static final String ISSUER_NAME = "Acme";
    private static final List<String> TOKEN_TYPES = List.of(
            "AcmeOps::Access_token", "AcmeOps::id_token", "AcmeOps::Userinfo_token");

    private AcmeStore() {
    }

    /**
     * Zips the policy store found in {@code storeDir}. When {@code storeDir} has no {@code metadata.json}, the
     * bundled policy store is used instead.
     */
    public static Path build(Path storeDir, String iss, String jwksUrl) throws IOException {
        boolean fromDisk = storeDir != null && Files.isRegularFile(storeDir.resolve("metadata.json"));
        if (fromDisk) {
            requireStoreFiles(storeDir);
            LOG.info("Policy store: {}", storeDir);
        } else {
            LOG.warn("No policy store at {} (metadata.json missing); using the bundled policy-store-acme", storeDir);
        }
        Path cjar = Files.createTempFile("policy-store-", ".cjar");
        try (OutputStream out = Files.newOutputStream(cjar); ZipOutputStream zip = new ZipOutputStream(out)) {
            if (fromDisk) {
                copyDirectory(storeDir, zip);
            } else {
                copyBundled(zip);
            }
            put(zip, "trusted-issuers.json", trustedIssuers(iss, jwksUrl));
        }
        return cjar;
    }

    private static void requireStoreFiles(Path dir) throws IOException {
        for (String required : List.of("schema.cedarschema", "policies")) {
            if (!Files.exists(dir.resolve(required))) {
                throw new IOException("policy store " + dir + " is missing " + required);
            }
        }
    }

    private static void copyDirectory(Path dir, ZipOutputStream zip) throws IOException {
        List<Path> files;
        try (java.util.stream.Stream<Path> walk = Files.walk(dir)) {
            files = walk.filter(Files::isRegularFile).sorted().toList();
        }
        for (Path file : files) {
            String name = dir.relativize(file).toString().replace('\\', '/');
            boolean wanted = name.equals("metadata.json") || name.equals("schema.cedarschema")
                    || name.startsWith("policies/");
            if (!wanted) {
                continue;
            }
            byte[] content = Files.readAllBytes(file);
            put(zip, name, name.equals("metadata.json") ? withSpecVersion(content) : content);
        }
    }

    private static void copyBundled(ZipOutputStream zip) throws IOException {
        for (String name : read(INDEX).lines().map(String::strip).filter(n -> !n.isEmpty()).toList()) {
            if (name.startsWith("trusted-issuers")) {
                continue;
            }
            byte[] content = bytes(RESOURCE_ROOT + name);
            put(zip, name, name.equals("metadata.json") ? withSpecVersion(content) : content);
        }
    }

    private static byte[] withSpecVersion(byte[] metadata) throws IOException {
        ObjectNode node = (ObjectNode) MAPPER.readTree(metadata);
        node.put("policy_store_spec_version", 2);
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(node);
    }

    private static byte[] trustedIssuers(String iss, String jwksUrl) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        for (String type : TOKEN_TYPES) {
            ObjectNode entry = root.putObject(type);
            entry.put("type", "jwt");
            entry.put("issuer", ISSUER_NAME);
            ObjectNode endpoints = entry.putObject("configuration_endpoints");
            endpoints.put("jwks", jwksUrl);
            endpoints.put("iss", iss);
            entry.put("token_id", "jti");
            entry.putArray("required_claims").add("iss").add("exp").add("jti").add("sub");
        }
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
    }

    private static String read(String resource) throws IOException {
        return new String(bytes(resource), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String resource) throws IOException {
        try (InputStream in = AcmeStore.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("missing resource " + resource);
            }
            return in.readAllBytes();
        }
    }

    private static void put(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }
}
