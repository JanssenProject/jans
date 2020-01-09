package org.gluu.oxd.server.op;

import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxd.server.service.PublicOpKeyService;
import org.gluu.oxd.server.service.Rp;

public class JwsSignerObject {

    private final Jwt idToken;
    private OpClientFactory opClientFactory;
    private final PublicOpKeyService keyService;
    private final Rp site;

    public JwsSignerObject(Jwt idToken, OpClientFactory opClientFactory, PublicOpKeyService keyService, Rp site) {
        this.idToken = idToken;
        this.opClientFactory = opClientFactory;
        this.keyService = keyService;
        this.site = site;
    }

    public Jwt getIdToken() {
        return idToken;
    }

    public OpClientFactory getOpClientFactory() {
        return opClientFactory;
    }

    public PublicOpKeyService getKeyService() {
        return keyService;
    }

    public Rp getSite() {
        return site;
    }
}
