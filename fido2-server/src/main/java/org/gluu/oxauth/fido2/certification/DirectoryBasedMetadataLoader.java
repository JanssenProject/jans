/*
 * Copyright (c) 2018 Mastercard
 * Copyright (c) 2018 Gluu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.gluu.oxauth.fido2.certification;

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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.service.DataMapperService;
import org.gluu.service.cdi.event.ApplicationInitialized;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.configuration.Fido2Configuration;

import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class DirectoryBasedMetadataLoader {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private DataMapperService dataMapperService;

    private Map<String, JsonNode> authenticatorsMetadata;

    @PostConstruct
    public void create() {
        this.authenticatorsMetadata = Collections.synchronizedMap(new HashMap());
    }

    public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) Object init) {
        Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
        if (fido2Configuration == null) {
            return;
        }

        String serverMetadataFolder = appConfiguration.getFido2Configuration().getServerMetadataFolder();

        log.info("Populating metadata from {}", serverMetadataFolder);
        authenticatorsMetadata.putAll(getAAGUIDMapOfMetadata());
    }

    private Map<String, JsonNode> getAAGUIDMapOfMetadata() {
        Map<String, JsonNode> nodes = Collections.synchronizedMap(new HashMap<>());

        String serverMetadataFolder = appConfiguration.getFido2Configuration().getServerMetadataFolder();
        if (StringHelper.isEmpty(serverMetadataFolder)) {
            log.debug("Property certificationServerMetadataFolder is empty");
            return nodes;
        }

        Path path = FileSystems.getDefault().getPath(serverMetadataFolder);
        DirectoryStream<Path> directoryStream = null;
        try {
            directoryStream = Files.newDirectoryStream(path);
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

        } catch (IOException e) {
            log.warn("Something wrong with path ", e);
        } finally {
            if (directoryStream != null) {
                try {
                    directoryStream.close();
                } catch (IOException e) {
                    log.warn("Something wrong with directory stream", e);
                }
            }
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
