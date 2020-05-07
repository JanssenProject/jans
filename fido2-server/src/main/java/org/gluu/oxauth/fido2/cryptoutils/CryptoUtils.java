/*
 * Copyright (c) 2018 Mastercard
 * Copyright (c) 2020 Gluu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.gluu.oxauth.fido2.cryptoutils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.service.Base64Service;
import org.slf4j.Logger;

@ApplicationScoped
public class CryptoUtils {

    @Inject
    private Logger log;

    @Inject
    private Base64Service base64Service;

    public X509Certificate getCertificate(String x509certificate) {
        return getCertificate(new ByteArrayInputStream(base64Service.decode(x509certificate)));

    }

    public X509Certificate getCertificate(InputStream is) {
        try {
        	X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        	certificate.checkValidity();
        	
        	return certificate;
        } catch (CertificateException e) {
            throw new Fido2RPRuntimeException(e.getMessage(), e);
        }
    }

    public List<X509Certificate> getCertificates(List<String> certificatePath) {
        final CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new Fido2RPRuntimeException(e.getMessage(), e);
        }

        return certificatePath.parallelStream().map(x509certificate -> {
            try {
                return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(base64Service.decode(x509certificate)));
            } catch (CertificateException e) {
                throw new Fido2RPRuntimeException(e.getMessage(), e);
            }
        }).filter(c -> {
            try {
                c.checkValidity();
                return true;
            } catch (CertificateException e) {
                log.warn("Certificate not valid {}", c.getIssuerDN().getName());
                throw new Fido2RPRuntimeException("Certificate not valid", e);
            }
        }).collect(Collectors.toList());

    }

    public List<X509Certificate> getCertificates(String rootCertificatePath) {
        ArrayList<X509Certificate> certificates = new ArrayList<X509Certificate>();
        Path path = FileSystems.getDefault().getPath(rootCertificatePath);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
            Iterator<Path> iter = directoryStream.iterator();
            while (iter.hasNext()) {
                Path filePath = iter.next();
                certificates.add(getCertificate(Files.newInputStream(filePath)));
            }
        } catch (Exception ex) {
            log.error("Failed to load cert from folder: '{}'", rootCertificatePath, ex);
        }
        
        return certificates;
    }

}
