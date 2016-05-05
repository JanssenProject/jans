/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.bouncycastle.jce.provider.JCERSAPrivateCrtKey;
import org.bouncycastle.jce.provider.JCERSAPublicKey;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.JwtHeader;
import org.xdi.oxauth.model.jwt.JwtType;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version December 17, 2015
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
    public void test() throws JSONException, NoSuchProviderException, NoSuchAlgorithmException, IOException, IllegalBlockSizeException, InvalidKeyException, InvalidKeySpecException, NoSuchPaddingException, BadPaddingException, InvalidJwtException {
        final JSONObject jsonHeader = JwtHeader.instance().setType(JwtType.JWT).setAlgorithm(SignatureAlgorithm.RS512).toJsonObject();
        final JSONObject jsonPayload = new JSONObject(TEST_METADATA);

        final KeyPair keyPair = JwtUtil.generateRsaKey();
        final JCERSAPrivateCrtKey jcersaPrivateCrtKey = (JCERSAPrivateCrtKey) keyPair.getPrivate();
        final JCERSAPublicKey jcersaPublicKey = (JCERSAPublicKey) keyPair.getPublic();

        final RSAPrivateKey privateKey = new RSAPrivateKey(
                jcersaPrivateCrtKey.getModulus(),
                jcersaPrivateCrtKey.getPrivateExponent());
        final RSAPublicKey publicKey = new RSAPublicKey(
                jcersaPublicKey.getModulus(),
                jcersaPublicKey.getPublicExponent());

        String encodedString = JwtUtil.encodeJwt(jsonHeader, jsonPayload, SignatureAlgorithm.RS512, privateKey);

        System.out.println("Encoded string: " + encodedString);
        String[] parts = encodedString.split("\\.");

        if (parts.length == 3) {
            String encodedHeader = parts[0];
            String encodedPayload = parts[1];
            String encodedSignature = parts[2];

            String header = new String(JwtUtil.base64urldecode(encodedHeader), Util.UTF8_STRING_ENCODING);
            String payload = new String(JwtUtil.base64urldecode(encodedPayload), Util.UTF8_STRING_ENCODING);
            System.out.println("Header: " + header);
            System.out.println("Payload: " + payload);

            byte[] signature = JwtUtil.base64urldecode(encodedSignature);

            final String signingInput = encodedHeader + "." + encodedPayload;

            boolean signatureVerified = JwtUtil.verifySignatureRS512(signingInput.getBytes(Util.UTF8_STRING_ENCODING), signature, publicKey);
            assertTrue(signatureVerified, "Invalid signature");
        }
    }
}
