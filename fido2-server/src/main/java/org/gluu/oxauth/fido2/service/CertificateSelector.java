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

package org.gluu.oxauth.fido2.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxauth.fido2.cryptoutils.CryptoUtilsBouncyCastle;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.slf4j.Logger;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.util.StringHelper;

@ApplicationScoped
public class CertificateSelector {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private CryptoUtilsBouncyCastle cryptoUtilsBouncyCastle;

    public List<X509Certificate> selectRootCertificate(X509Certificate certificate) {
        ArrayList<X509Certificate> certs = new ArrayList<>();

        String certsFolder = appConfiguration.getFido2Configuration().getCertFilesFolder();
        if (StringHelper.isEmpty(certsFolder)) {
            log.warn("Fido2 folder with certificates is not specified");
            return certs;
        }

        Provider provider = cryptoUtilsBouncyCastle.getBouncyCastleProvider();

        try {
            switch (certificate.getIssuerDN().getName()) {
            case "CN=Yubico U2F Root CA Serial 457200631":
                certs.add((X509Certificate) CertificateFactory.getInstance("X509", provider)
                        .generateCertificate(new FileInputStream(new File(certsFolder + "yubico-u2f-ca-certs.crt"))));
                break;

            case "L=Wakefield, ST=MY, C=US, OU=CWG, O=FIDO Alliance, EMAILADDRESS=conformance-tools@fidoalliance.org, CN=FIDO2 BATCH KEY prime256v1":
                certs.add((X509Certificate) CertificateFactory.getInstance("X509", provider)
                        .generateCertificate(new FileInputStream(new File(certsFolder + "fido-conf-tool-ca-batch-cert.crt"))));
            case "L=Wakefield, ST=MY, C=US, OU=CWG, O=FIDO Alliance, EMAILADDRESS=conformance-tools@fidoalliance.org, CN=FIDO2 INTERMEDIATE prime256v1":
                certs.add((X509Certificate) CertificateFactory.getInstance("X509", provider)
                        .generateCertificate(new FileInputStream(new File(certsFolder + "fido-conf-tool-ca-intermediate-cert.crt"))));
            case "L=Wakefield, ST=MY, C=US, OU=CWG, O=FIDO Alliance, EMAILADDRESS=conformance-tools@fidoalliance.org, CN=FIDO2 TEST ROOT":
                certs.add((X509Certificate) CertificateFactory.getInstance("X509", provider)
                        .generateCertificate(new FileInputStream(new File(certsFolder + "fido-conf-tool-ca-root-cert.crt"))));
                break;
            default:
                throw new Fido2RPRuntimeException("Can't find certificate");
            }
        } catch (CertificateException | FileNotFoundException e) {
            log.info("Problem {} ", e.getMessage());
            throw new Fido2RPRuntimeException("Can't validate certificate");
        }

        return certs;

    }
}
