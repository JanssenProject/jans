/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import org.codehaus.jettison.json.JSONObject;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseComponentTestAdapter;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.crypto.AbstractCryptoProvider;
import org.xdi.oxauth.model.crypto.CryptoProviderFactory;
import org.xdi.oxauth.model.crypto.signature.RSAKeyFactory;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwk.JSONWebKey;
import org.xdi.oxauth.model.jws.RSASigner;
import org.xdi.oxauth.model.jwt.*;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * https://localhost:8443/oxauth/seam/resource/restv1/oxauth/jwk
 * http://openid.net/specs/openid-connect-messages-1_0.html#sigs
 *
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version June 28, 2016
 */

public class FederationSigningTest extends BaseComponentTestAdapter {

    private static final String TEST_METADATA = "{\"federation_id\"  : \"@!2222!0008!FF8F!7434\",\n" +
            " \"display_name\" : \"Federation example name\",\n" +
            " \"OPs\" : [\n" +
            "          {\n" +
            "            \"display_name\" : \"Example OP\",\n" +
            "            \"op_id\" : \"example.com\",\n" +
            "            \"domain\" : \"example.com\"\n" +
            "          }          \n" +
            "         ],\n" +
            " \"RPs\" : [\n" +
            "          {\n" +
            "            \"display_name\" : \"oxGraph client\",\n" +
            "            \"redirect_uri\" : \"example.com/oxGraph\"\n" +
            "          }\n" +
            "         ]        \n" +
            "}";

    @Test
    public void test() {
        try {
            final String keyId = testKeyId();
            final SignatureAlgorithm algorithm = SignatureAlgorithm.fromString(ConfigurationFactory.instance().getConfiguration().getFederationSigningAlg());

            final JSONWebKey JSONWebKey = ConfigurationFactory.instance().getWebKeys().getKey(keyId);

            final RSAKeyFactory factory = RSAKeyFactory.valueOf(JSONWebKey);

            final JSONObject jsonHeader = JwtHeader.instance().
                    setType(JwtType.JWT).setAlgorithm(algorithm).setKeyId(keyId).
                    toJsonObject();
            final JSONObject jsonPayload = new JSONObject(TEST_METADATA);

            AbstractCryptoProvider cryptoProvider = CryptoProviderFactory.getCryptoProvider(ConfigurationFactory.instance().getConfiguration());

            String header = jsonHeader.toString();
            String payload = jsonPayload.toString();
            header = JwtUtil.base64urlencode(header.getBytes(Util.UTF8_STRING_ENCODING));
            payload = JwtUtil.base64urlencode(payload.getBytes(Util.UTF8_STRING_ENCODING));
            final String signingInput = header + "." + payload;

            final String signature = cryptoProvider.sign(signingInput, keyId, null, SignatureAlgorithm.RS512);
            final String signedJwt = signingInput + "." + signature;

            ////////////// VERIFICATION //////////////
            final PureJwt jwt = PureJwt.parse(signedJwt);

            // 1. check signing
            RSASigner rsaSigner = new RSASigner(algorithm, factory.getPublicKey());
            assertTrue(rsaSigner.validateSignature(jwt.getSigningInput(), jwt.getEncodedSignature()));//

            // 2. chtestKeyIdeyId and jwtPath
            final JwtHeader jwtHeader = Jwt.parse(signedJwt).getHeader();
            assertTrue(jwtHeader.getClaim(JwtHeaderName.KEY_ID).equals(keyId));
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    public static String testKeyId() {
        String keyId = ConfigurationFactory.instance().getConfiguration().getFederationSigningKid();

        if (ConfigurationFactory.instance().getWebKeys().getKey(keyId) != null) {
            return keyId;
        }

        keyId = "6898cff9-4f92-4b58-b37c-2a2b6779b0b3";
        if (ConfigurationFactory.instance().getWebKeys().getKey(keyId) != null) {
            return keyId;
        }

        if (!ConfigurationFactory.instance().getWebKeys().getKeys().isEmpty()) {
            return ConfigurationFactory.instance().getWebKeys().getKeys().get(0).getKid();
        }

        throw new RuntimeException("Failed to identify key id for signing");

    }
}
