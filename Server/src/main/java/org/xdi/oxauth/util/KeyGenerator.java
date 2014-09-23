package org.xdi.oxauth.util;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.crypto.Key;
import org.xdi.oxauth.model.crypto.KeyFactory;
import org.xdi.oxauth.model.crypto.signature.*;

/**
 * @author Javier Rojas Blum
 * @version 0.9, 09/23/2014
 */
public class KeyGenerator {

    public static void main(String[] args) throws Exception {
        JSONArray keys = new JSONArray();

        keys.put(generateRS256Keys());
        keys.put(generateRS384Keys());
        keys.put(generateRS512Keys());

        keys.put(generateES256Keys());
        keys.put(generateES384Keys());
        keys.put(generateES512Keys());

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("keys", keys);

        System.out.println(jsonObject.toString(4).replace("\\/", "/"));
    }

    public static JSONObject generateRS256Keys() throws Exception {
        KeyFactory<RSAPrivateKey, RSAPublicKey> keyFactory = new RSAKeyFactory(
                SignatureAlgorithm.RS256,
                "CN=Test CA Certificate");

        Key<RSAPrivateKey, RSAPublicKey> key = keyFactory.getKey();

        key.setKeyType("RSA");
        key.setUse("SIGNATURE");
        key.setAlgorithm("RS256");
        key.setKeyId("1");
        key.setCurve(JSONObject.NULL);

        return key.toJSONObject();
    }

    public static JSONObject generateRS384Keys() throws Exception {
        KeyFactory<RSAPrivateKey, RSAPublicKey> keyFactory = new RSAKeyFactory(
                SignatureAlgorithm.RS384,
                "CN=Test CA Certificate");

        Key<RSAPrivateKey, RSAPublicKey> key = keyFactory.getKey();

        key.setKeyType("RSA");
        key.setUse("SIGNATURE");
        key.setAlgorithm("RS384");
        key.setKeyId("2");
        key.setCurve(JSONObject.NULL);

        return key.toJSONObject();
    }

    public static JSONObject generateRS512Keys() throws Exception {
        KeyFactory<RSAPrivateKey, RSAPublicKey> keyFactory = new RSAKeyFactory(
                SignatureAlgorithm.RS512,
                "CN=Test CA Certificate");

        Key<RSAPrivateKey, RSAPublicKey> key = keyFactory.getKey();

        key.setKeyType("RSA");
        key.setUse("SIGNATURE");
        key.setAlgorithm("RS512");
        key.setKeyId("3");
        key.setCurve(JSONObject.NULL);

        return key.toJSONObject();
    }

    public static JSONObject generateES256Keys() throws Exception {
        KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> keyFactory = new ECDSAKeyFactory(
                SignatureAlgorithm.ES256,
                "CN=Test CA Certificate");

        Key<ECDSAPrivateKey, ECDSAPublicKey> key = keyFactory.getKey();

        key.setKeyType("EC");
        key.setUse("SIGNATURE");
        key.setAlgorithm("EC");
        key.setKeyId("4");
        key.setCurve("P-256");

        return key.toJSONObject();
    }

    public static JSONObject generateES384Keys() throws Exception {
        KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> keyFactory = new ECDSAKeyFactory(
                SignatureAlgorithm.ES384,
                "CN=Test CA Certificate");

        Key<ECDSAPrivateKey, ECDSAPublicKey> key = keyFactory.getKey();

        key.setKeyType("EC");
        key.setUse("SIGNATURE");
        key.setAlgorithm("EC");
        key.setKeyId("5");
        key.setCurve("P-384");

        return key.toJSONObject();
    }

    public static JSONObject generateES512Keys() throws Exception{
        KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> keyFactory = new ECDSAKeyFactory(
                SignatureAlgorithm.ES512,
                "CN=Test CA Certificate");

        Key<ECDSAPrivateKey, ECDSAPublicKey> key = keyFactory.getKey();

        key.setKeyType("EC");
        key.setUse("SIGNATURE");
        key.setAlgorithm("EC");
        key.setKeyId("6");
        key.setCurve("P-521");

        return key.toJSONObject();
    }
}
