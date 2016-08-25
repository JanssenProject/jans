/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.codehaus.jettison.json.JSONObject;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.crypto.OxAuthCryptoProvider;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwt.JwtHeader;
import org.xdi.oxauth.model.jwt.JwtType;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.Util;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version August 5, 2016
 */

public class FederationMetadataSignatureTest {

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
    @Parameters({"RS512_keyId", "keyStoreFile", "keyStoreSecret"})
    public void test(final String keyId, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        try {
            final JSONObject jsonHeader = JwtHeader.instance().setType(JwtType.JWT).setAlgorithm(SignatureAlgorithm.RS512).toJsonObject();
            final JSONObject jsonPayload = new JSONObject(TEST_METADATA);

            final String header = Base64Util.base64urlencode(jsonHeader.toString().getBytes(Util.UTF8_STRING_ENCODING));
            final String payload = Base64Util.base64urlencode(jsonPayload.toString().getBytes(Util.UTF8_STRING_ENCODING));

            final String signingInput = header + "." + payload;

            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            String encodedSignature = cryptoProvider.sign(signingInput, keyId, null, SignatureAlgorithm.RS512);

            boolean signatureVerified = cryptoProvider.verifySignature(signingInput, encodedSignature, keyId, null, null, SignatureAlgorithm.RS512);
            assertTrue(signatureVerified);
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }
}
