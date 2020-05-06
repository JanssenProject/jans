package org.gluu.oxauth.fido2.service.mds;

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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.service.DataMapperService;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.configuration.Fido2Configuration;
import org.gluu.service.cdi.event.ApplicationInitialized;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

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
                        log.info("AAGUID conversion old {} new {}", aaguid, convertedAaguid);
                        nodes.put(convertedAaguid, jsonNode);
                    } else {
                        log.info("No aaguid for file path {}", filePath);
                    }
                } catch (IOException e) {
                    log.warn("Can't process {}", filePath, e);
                }
            }
        } catch (IOException ex) {
            log.warn("Something wrong with path ", ex);
        }

        return nodes;
    }

    public void registerAuthenticatorsMetadata(String aaguid, JsonNode metadata) {
        this.authenticatorsMetadata.put(aaguid, metadata);
    }

    public JsonNode getAuthenticatorsMetadata(String aaguid) {
        return authenticatorsMetadata.get(aaguid);
    }

}
