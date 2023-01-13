/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.legacy.service.util;

import io.jans.as.model.crypto.Certificate;
import io.jans.as.model.crypto.Key;
import io.jans.as.model.crypto.signature.ECDSAKeyFactory;
import io.jans.as.model.crypto.signature.ECDSAPrivateKey;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Use;
import io.jans.as.model.util.SecurityProviderUtility;
import org.json.JSONObject;
import org.python.icu.util.Calendar;

import java.util.Date;
import java.util.UUID;

/**
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version August 28, 2017
 */
public class KeyGenerator {
    public static void main(String[] args) throws Exception {
        SecurityProviderUtility.installBCProvider(true);

        Calendar cal = Calendar.getInstance();
        Date startDate = cal.getTime();

        cal.add(Calendar.YEAR, 3);
        Date expirationDate = cal.getTime();

        String dnName = "C=US,ST=TX,L=Austin,o=jans,CN=Gluu oxPush2 U2F v1.0.0";

        generateU2fAttestationKeys(startDate, expirationDate, dnName);
    }

    public static void generateU2fAttestationKeys(Date startDate, Date expirationDate, String dnName) throws Exception {
        ECDSAKeyFactory keyFactory = new ECDSAKeyFactory(
                SignatureAlgorithm.ES256,
                null);
        Key<ECDSAPrivateKey, ECDSAPublicKey> key = keyFactory.getKey();
        Certificate certificate = keyFactory.generateV3Certificate(startDate, expirationDate, dnName);
        key.setCertificate(certificate);

        key.setKeyType(SignatureAlgorithm.ES256.getFamily().getValue());
        key.setUse(Use.SIGNATURE.toString());
        key.setAlgorithm(SignatureAlgorithm.ES256.getName());
        key.setKeyId(UUID.randomUUID().toString());
        key.setExpirationTime(expirationDate.getTime());
        key.setCurve(SignatureAlgorithm.ES256.getCurve());

        JSONObject jsonKey = key.toJSONObject();
        System.out.println(jsonKey);

        System.out.println("CERTIFICATE:");
        System.out.println(certificate);
    }

}
