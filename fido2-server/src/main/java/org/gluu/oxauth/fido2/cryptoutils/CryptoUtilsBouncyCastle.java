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

package org.gluu.oxauth.fido2.cryptoutils;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.gluu.oxauth.fido2.service.Base64Service;
import org.slf4j.Logger;

@Deprecated
@ApplicationScoped
public class CryptoUtilsBouncyCastle {

    @Inject
    private Logger log;

    @Inject
    private Base64Service base64Service;

    public X509Certificate getCertificate(String x5c) {
        try {
            return (X509Certificate) new CertificateFactory().engineGenerateCertificate(new ByteArrayInputStream(base64Service.decode(x5c)));
        } catch (CertificateException e) {
            throw new Fido2RPRuntimeException(e.getMessage());
        }
    }

    public List<X509Certificate> getCertificates(ArrayList<String> certificatePath) {
        return certificatePath.parallelStream().map(f -> getCertificate(f)).filter(c -> {
            try {
                c.checkValidity();
                PublicKey key = c.getPublicKey();
                return true;
            } catch (CertificateException e) {
                log.warn("Certificate not valid {}", c.getIssuerDN().getName());
                throw new Fido2RPRuntimeException("Certificate not valid ");
            }
        }).collect(Collectors.toList());
    }

}
