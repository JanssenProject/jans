/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.service.policy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import io.jans.core.cedarling.service.policy.PolicyStoreFileProvider;
import io.jans.lock.model.config.AppConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Extracts the bundled {@code lock-policy-store.zip} classpath resource to a
 * temporary zip file on the filesystem, patching
 * {@code trusted-issuers/jans-issuer.json} with the actual OpenID issuer URL
 * taken from {@link AppConfiguration#getOpenIdIssuer()}.
 *
 * @author Yuriy Movchan Date: 06/26/2026
 */
@ApplicationScoped
public class LockPolicyStoreFileProvider implements PolicyStoreFileProvider {

    private static final String POLICY_STORE_RESOURCE = "lock-policy-store.zip";
    private static final String JANS_ISSUER_ENTRY = "trusted-issuers/jans-issuer.json";
    private static final String CONFIGURATION_ENDPOINT_KEY = "configuration_endpoint";
    private static final String WELL_KNOWN_PATH = "/.well-known/openid-configuration";

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    private Path tempZipFile;

    @Override
    public void prepare() {
        String openIdIssuer = appConfiguration.getOpenIdIssuer();
        if (openIdIssuer == null || openIdIssuer.isBlank()) {
            throw new IllegalStateException("Cannot prepare policy store: appConfiguration.openIdIssuer is not set");
        }

        try {
            tempZipFile = Files.createTempFile("lock-policy-store-", ".cjar");
            log.info("Preparing policy store from classpath resource '{}' (issuer: {})", POLICY_STORE_RESOURCE, openIdIssuer);

            copyAndPatchZip(openIdIssuer);

            log.info("Policy store prepared at: {}", tempZipFile);
        } catch (IOException ex) {
            cleanup();
            throw new RuntimeException("Failed to prepare policy store from classpath resource: " + POLICY_STORE_RESOURCE, ex);
        }
    }

    @Override
    public String getPolicyStorePath() {
        return tempZipFile.toAbsolutePath().toString();
    }

    @Override
    public void cleanup() {
        if (tempZipFile != null) {
            try {
                Files.deleteIfExists(tempZipFile);
                log.info("Policy store temp file removed: {}", tempZipFile);
            } catch (IOException ex) {
                log.warn("Failed to delete policy store temp file: {}", tempZipFile, ex);
            }
            tempZipFile = null;
        }
    }

    private void copyAndPatchZip(String openIdIssuer) throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(POLICY_STORE_RESOURCE)) {
            if (resource == null) {
                throw new IOException("Classpath resource not found: " + POLICY_STORE_RESOURCE);
            }

            try (ZipInputStream zis = new ZipInputStream(resource);
                 OutputStream out = Files.newOutputStream(tempZipFile);
                 ZipOutputStream zos = new ZipOutputStream(out)) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    zos.putNextEntry(new ZipEntry(entry.getName()));

                    if (JANS_ISSUER_ENTRY.equals(entry.getName())) {
                        byte[] patched = patchIssuerJson(IOUtils.toByteArray(zis), openIdIssuer);
                        zos.write(patched);
                        log.debug("Patched {} with issuer URL: {}", JANS_ISSUER_ENTRY, openIdIssuer + WELL_KNOWN_PATH);
                    } else {
                        IOUtils.copy(zis, zos);
                    }

                    zos.closeEntry();
                    zis.closeEntry();
                }
            }
        }
    }

    private byte[] patchIssuerJson(byte[] original, String openIdIssuer) throws IOException {
        try {
            JSONObject json = new JSONObject(new String(original, StandardCharsets.UTF_8));
            json.put(CONFIGURATION_ENDPOINT_KEY, openIdIssuer + WELL_KNOWN_PATH);
            return json.toString(2).getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IOException("Failed to patch " + JANS_ISSUER_ENTRY + " with issuer URL: " + openIdIssuer, ex);
        }
    }
}
