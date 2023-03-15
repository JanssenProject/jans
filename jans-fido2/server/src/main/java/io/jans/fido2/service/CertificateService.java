/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import io.jans.fido2.exception.Fido2RuntimeException;
import org.slf4j.Logger;

/**
 * Utiltiy class for Certificate related operations
 * @author Yuriy Movchan
 * @version May 08, 2020
 */

@ApplicationScoped
public class CertificateService {

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
            throw new Fido2RuntimeException(e.getMessage(), e);
        }
    }

    public List<X509Certificate> getCertificates(List<String> certificatePath, boolean checkValidaty) {
        final CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new Fido2RuntimeException(e.getMessage(), e);
        }

        return certificatePath.parallelStream().map(x509certificate -> {
            try {
                return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(base64Service.decode(x509certificate)));
            } catch (CertificateException e) {
                throw new Fido2RuntimeException(e.getMessage(), e);
            }
        }).filter(c -> {
        	if (checkValidaty) {
	            try {
	                c.checkValidity();
	                return true;
	            } catch (CertificateException e) {
	                log.warn("Certificate not valid {}", c.getIssuerDN().getName());
	                throw new Fido2RuntimeException("Certificate not valid", e);
	            }
        	}
            return true;
        }).collect(Collectors.toList());

    }

    public List<X509Certificate> getCertificates(List<String> certificatePath) {
    	return getCertificates(certificatePath, true);
    }

    public Map<String, X509Certificate> getCertificatesMap(String rootCertificatePath) {
    	List<X509Certificate> certificates = getCertificates(rootCertificatePath);
    	
    	Map<String, X509Certificate> certificatesMap = new HashMap<String, X509Certificate>(certificates.size());
    	for (X509Certificate certificate : certificates) {
            String subjectDn = certificate.getSubjectDN().getName().toLowerCase();
    		
    		certificatesMap.put(subjectDn, certificate);
    	}
    	
    	return certificatesMap;
    }

    public List<X509Certificate> getCertificates(String rootCertificatePath) {
        ArrayList<X509Certificate> certificates = new ArrayList<X509Certificate>();
        Path path = FileSystems.getDefault().getPath(rootCertificatePath);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
            Iterator<Path> iter = directoryStream.iterator();
            while (iter.hasNext()) {
                Path filePath = iter.next();
                if (!Files.isDirectory(filePath)) {
					certificates.add(getCertificate(Files.newInputStream(filePath)));
				}
            }
        } catch (Exception ex) {
            log.error("Failed to load cert from folder: '{}'", rootCertificatePath, ex);
        }
        
        return certificates;
    }

    public X509Certificate getCertificate(String certsFolder, String certFileName) {
        Path certFilePath = FileSystems.getDefault().getPath(certsFolder).resolve(certFileName);

        try (InputStream certFileReader = Files.newInputStream(certFilePath)) {
            return getCertificate(certFileReader);
        } catch (IOException ex) {
            log.error("Faield to load certificates from folder {} with name {}", certFilePath, certFileName, ex);
            throw new Fido2RuntimeException("Can't load authenticator certificate. Certificate doen't exist!");
		}
    }

	public List<X509Certificate> selectRootCertificates(Map<String, X509Certificate> trustChainCertificatesMap,
			List<X509Certificate> certificates) {

		List<X509Certificate> selecedCertificates = new ArrayList<X509Certificate>();
		for (X509Certificate certificate : certificates) {
            String issuerDn = certificate.getIssuerDN().getName().toLowerCase();
            if (trustChainCertificatesMap.containsKey(issuerDn)) {
            	X509Certificate rootCert = trustChainCertificatesMap.get(issuerDn);
            	selecedCertificates.add(rootCert);
            }
		}

		return selecedCertificates;
	}

    public void saveCertificate(X509Certificate certificate) {
        try {
            Writer writer = IOUtils.buffer(new FileWriter(new File("/tmp/cert-" + certificate.getSerialNumber() + ".crt")));
            JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
            try {
                pemWriter.writeObject(certificate);
                pemWriter.flush();
            } finally {
                IOUtils.closeQuietly(pemWriter);
            }
        } catch (IOException e) {
            throw new Fido2RuntimeException("Failed to write root certificate");
        }
    }

}
