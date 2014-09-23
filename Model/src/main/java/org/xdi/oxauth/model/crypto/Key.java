/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.common.JSONable;
import org.xdi.oxauth.model.util.StringUtils;

/**
 * @author Javier Rojas Blum Date: 13.01.2013
 */
public class Key<E extends  PrivateKey, F extends PublicKey> implements JSONable {

    private E privateKey;
    private F publicKey;
    private Certificate certificate;

    public E getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(E privateKey) {
        this.privateKey = privateKey;
    }

    public F getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(F publicKey) {
        this.publicKey = publicKey;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        /*jsonObject.put("keyType");
        jsonObject.put("use");
        jsonObject.put("Algorithm");
        jsonObject.put("keyId");*/
        //jsonObject.put("curve", getPublicKey().);
        jsonObject.put("privateKey", getPrivateKey().toJSONObject());
        jsonObject.put("publicKey", getPublicKey().toJSONObject());
        jsonObject.put("certificateChain", getCertificate().toJSONArray());

        return jsonObject;
    }

    @Override
    public String toString() {
        try {
            return toJSONObject().toString(4).replace("\\/", "/");
        } catch (JSONException e) {
            return StringUtils.EMPTY_STRING;
        } catch (Exception e) {
            return StringUtils.EMPTY_STRING;
        }
    }
}