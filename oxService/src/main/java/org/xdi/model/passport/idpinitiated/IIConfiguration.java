package org.xdi.model.passport.idpinitiated;

import java.util.List;

/**
 * Created by jgomer on 2019-02-21.
 */
public class IIConfiguration {

    private OIDCDetails openidclient;
    private List<AuthzParams> authorizationParams;

    public OIDCDetails getOpenidclient() {
        return openidclient;
    }

    public void setOpenidclient(OIDCDetails openidclient) {
        this.openidclient = openidclient;
    }

    public List<AuthzParams> getAuthorizationParams() {
        return authorizationParams;
    }

    public void setAuthorizationParams(List<AuthzParams> authorizationParams) {
        this.authorizationParams = authorizationParams;
    }

}
