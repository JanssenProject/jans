package org.gluu.oxeleven.rest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxeleven.model.Configuration;
import org.gluu.oxeleven.service.ConfigurationService;
import org.gluu.oxeleven.service.PKCS11Service;
import org.gluu.oxeleven.util.Base64Util;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import sun.security.ec.ECPublicKeyImpl;
import sun.security.rsa.RSAPublicKeyImpl;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;

import static org.gluu.oxeleven.model.JwksResponseParam.*;

/**
 * @author Javier Rojas Blum
 * @version April 4, 2016
 */
@Name("jwksRestService")
public class JwksRestServiceImpl implements JwksRestService {

    @Logger
    private Log log;

    @Override
    public Response sign(List<String> aliasList) {
        Response.ResponseBuilder builder = Response.ok();

        try {
            if (aliasList == null || aliasList.isEmpty()) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity("The request asked for an operation that cannot be supported because the provided aliasList parameter is mandatory.");
            } else {
                Configuration configuration = ConfigurationService.instance().getConfiguration();
                String pkcs11Pin = configuration.getPkcs11Pin();
                Map<String, String> pkcs11Config = configuration.getPkcs11Config();

                PKCS11Service pkcs11 = new PKCS11Service(pkcs11Pin, pkcs11Config);
                String response = getJSonResponse(pkcs11, aliasList);
                builder.entity(response);
            }
        } catch (CertificateException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (KeyStoreException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (UnrecoverableEntryException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (JSONException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        }

        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoTransform(false);
        cacheControl.setNoStore(true);
        builder.cacheControl(cacheControl);
        builder.header("Pragma", "no-cache");
        return builder.build();
    }

    private String getJSonResponse(PKCS11Service pkcs11, List<String> aliasList)
            throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException, JSONException {
        JSONObject jsonObj = new JSONObject();
        JSONArray keys = new JSONArray();
        for (String alias : aliasList) {
            JSONObject jsonKeyValue = new JSONObject();
            jsonKeyValue.put(KEY_ID, alias);
            jsonKeyValue.put(KEY_USE, "sig");
            PublicKey publicKey = pkcs11.getPublicKey(alias);
            if (publicKey != null) {
                publicKey.getAlgorithm();
                if (publicKey instanceof RSAPublicKeyImpl) {
                    jsonKeyValue.put(KEY_TYPE, "RSA");
                    RSAPublicKeyImpl key = (RSAPublicKeyImpl) publicKey;
                    jsonKeyValue.put(MODULUS, Base64Util.base64UrlEncodeUnsignedBigInt(key.getModulus()));
                    jsonKeyValue.put(EXPONENT, Base64Util.base64UrlEncodeUnsignedBigInt(key.getPublicExponent()));
                } else if (publicKey instanceof ECPublicKeyImpl) {
                    jsonKeyValue.put(KEY_TYPE, "EC");
                    ECPublicKeyImpl key = (ECPublicKeyImpl) publicKey;
                    jsonKeyValue.put(X, Base64Util.base64UrlEncodeUnsignedBigInt(key.getW().getAffineX()));
                    jsonKeyValue.put(Y, Base64Util.base64UrlEncodeUnsignedBigInt(key.getW().getAffineY()));
                }
                keys.put(jsonKeyValue);
            }
        }
        jsonObj.put(JSON_WEB_KEY_SET, keys);

        return jsonObj.toString();
    }
}
