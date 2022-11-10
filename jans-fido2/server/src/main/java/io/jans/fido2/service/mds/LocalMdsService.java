/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.mds;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.service.DataMapperService;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The FIDO2 server has a local database of authenticator data in json format.
 * It is parsed before MDS blob is looked up. This data has to be obtained from
 * the vendor and placed in the local folder for metadata
 * 
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class LocalMdsService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private DataMapperService dataMapperService;

    private Map<String, JsonNode> authenticatorsMetadata;

    public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) Object init) {
        this.authenticatorsMetadata = Collections.synchronizedMap(new HashMap<String, JsonNode>());

        Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
        if (fido2Configuration == null) {
            return;
        }

        String serverMetadataFolder = appConfiguration.getFido2Configuration().getServerMetadataFolder();

        log.info("Populating metadata from {}", serverMetadataFolder);
        authenticatorsMetadata.putAll(getAAGUIDMapOfMetadata(serverMetadataFolder));
    }

    private Map<String, JsonNode> getAAGUIDMapOfMetadata(String serverMetadataFolder) {
        Map<String, JsonNode> nodes = Collections.synchronizedMap(new HashMap<>());

        if (StringHelper.isEmpty(serverMetadataFolder)) {
            log.debug("Property certificationServerMetadataFolder is empty");
            return nodes;
        }

        Path path = FileSystems.getDefault().getPath(serverMetadataFolder);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
            Iterator<Path> iter = directoryStream.iterator();
            while (iter.hasNext()) {
                Path filePath = iter.next();
                try {
                    log.info("Reading file {}", filePath);
                    BufferedReader reader = Files.newBufferedReader(filePath);
                    JsonNode jsonNode = dataMapperService.readTree(reader);
                    if (jsonNode.hasNonNull("aaguid")) {
                        String aaguid = jsonNode.get("aaguid").asText();
                        String convertedAaguid = aaguid.replaceAll("-", "");
                        log.debug("AAGUID conversion old {} new {}", aaguid, convertedAaguid);
                        nodes.put(convertedAaguid, jsonNode);
                    } else {
                        log.debug("No aaguid for file path {}", filePath);
                    }
                } catch (IOException ex) {
                    log.warn("Can't process {}", filePath, ex);
                }
            }
        } catch (IOException ex) {
            log.warn("Something wrong with path ", ex);
        }

        return nodes;
    }

    public JsonNode getAuthenticatorsMetadata(String aaguid) {
        return authenticatorsMetadata.get(aaguid);
    }

}
