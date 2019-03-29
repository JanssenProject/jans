/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.oxauth.fido2.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.cryptoutils.CryptoUtils;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.configuration.Fido2Configuration;
import org.gluu.service.cdi.event.ApplicationInitialized;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class CertificateSelector {

    @Inject
    private Logger log;

    @Inject
    private DataMapperService dataMapperService;
    
    @Inject
    private CryptoUtils cryptoUtils;

    @Inject
    private AppConfiguration appConfiguration;

    private Map<String, List<X509Certificate>> certMapping;

    @PostConstruct
    public void create() {
        this.certMapping = new HashMap<String, List<X509Certificate>>();
    }

    public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) Object init) {
        certMapping.putAll(parseMapping());
    }

    public List<X509Certificate> selectRootCertificate(X509Certificate certificate) {
        ArrayList<X509Certificate> certs = new ArrayList<>();

        Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
        if (fido2Configuration == null) {
            log.warn("Fido2 authenticator folder with certificates is not specified");
            return certs;
        }

        String certsFolder = fido2Configuration.getAuthenticatorCertsFolder();
        if (StringHelper.isEmpty(certsFolder)) {
            log.warn("Fido2 authenticator folder with certificates is not specified");
            return certs;
        }


        String issuerDn = certificate.getIssuerDN().getName();
        List<X509Certificate> cert = this.certMapping.get(issuerDn.toLowerCase());
        
        certs.addAll(certs);

        if (certs.size() == 0) {
            log.warn("Fido2 authenticator with issuer Dn: '{}' not registered in mapping file");
            return certs;
        }

        return certs;
    }

    private Map<String, List<X509Certificate>> parseMapping() {
        Map<String, List<X509Certificate>> mappings = new HashMap<String, List<X509Certificate>>();

        Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
        if (fido2Configuration == null) {
            log.warn("Fido2 authenticator folder with certificates is not specified");
            return mappings;
        }

        String certsFolder = appConfiguration.getFido2Configuration().getAuthenticatorCertsFolder();
        if (StringHelper.isEmpty(certsFolder)) {
            log.warn("Fido2 authenticator folder with certificates is not specified");
            return mappings;
        }

        Path certsFolderPath = FileSystems.getDefault().getPath(certsFolder);
        
        DirectoryStream<Path> directoryStream = null;
        try {
            directoryStream = Files.newDirectoryStream(certsFolderPath, "*.json");
            Iterator<Path> iter = directoryStream.iterator();
            while (iter.hasNext()) {
                Path filePath = iter.next();
                BufferedReader reader = null;
                try {
                    reader = Files.newBufferedReader(filePath);
                    JsonNode jsonNodes = dataMapperService.readTree(reader);
                    for (JsonNode jsonNode : jsonNodes) {
                        if (jsonNode.hasNonNull("issuer") && jsonNode.hasNonNull("cert_file")) {
                            String issuer = jsonNode.get("issuer").asText().toLowerCase(); 
                            String certFile = jsonNode.get("cert_file").asText(); 
                            
                            List<X509Certificate> certs = mappings.get(issuer);
                            if (certs == null) {
                                certs = new ArrayList<X509Certificate>();    
                                mappings.put(issuer, certs);
                            }

                            X509Certificate cert = getCertificate(certsFolder, certFile);
                            certs.add(cert);
                        }
                    }
                } catch (IOException e) {
                    log.info("Unable to read authenticator certificates mapping file {} ", e.getMessage(), e);
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            log.warn("Unable to close reader {}", reader);
                        }
                    }
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

        return mappings;
    }

    private X509Certificate getCertificate(String certsFolder, String certFileName) {
        Path certFilePath = FileSystems.getDefault().getPath(certsFolder).resolve(certFileName);
        InputStream certFileReader;
        try {
            certFileReader = Files.newInputStream(certFilePath);
        } catch (IOException e) {
            log.info("Problem {} ", e.getMessage(), e);
            throw new Fido2RPRuntimeException("Can't load authenticator certificate. Certificate doen't exist!");
        }

        try {
            return cryptoUtils.getCertificate(certFileReader);
        } finally {
            if (certFileReader != null) {
                try {
                    certFileReader.close();
                } catch (IOException e) {
                    log.warn("Unable to close reader {}", certFileReader);
                }
            }
        }
    }

}
