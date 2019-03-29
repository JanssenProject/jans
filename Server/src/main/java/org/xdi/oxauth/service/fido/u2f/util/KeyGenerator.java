/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.fido.u2f.util;

import java.util.Date;
import java.util.UUID;

import org.codehaus.jettison.json.JSONObject;
import org.python.icu.util.Calendar;
import org.xdi.oxauth.model.crypto.Certificate;
import org.xdi.oxauth.model.crypto.Key;
import org.xdi.oxauth.model.crypto.signature.ECDSAKeyFactory;
import org.xdi.oxauth.model.crypto.signature.ECDSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwk.Use;
import org.xdi.oxauth.model.util.SecurityProviderUtility;

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

        String dnName = "C=US,ST=TX,L=Austin,O=Gluu,CN=Gluu oxPush2 U2F v1.0.0";

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
