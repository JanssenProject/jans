package io.jans.as.client.par;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.ClientAuthnRequest;
import io.jans.as.model.common.AuthorizationMethod;
import io.jans.as.model.util.QueryBuilder;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ParRequest extends ClientAuthnRequest {

    private AuthorizationRequest authorizationRequest;
    private Integer nbf;

    public ParRequest(AuthorizationRequest authorizationRequest) {
        this.authorizationRequest = authorizationRequest;
        setAuthorizationMethod(AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER);
    }

    public AuthorizationRequest getAuthorizationRequest() {
        return authorizationRequest;
    }

    public void setAuthorizationRequest(AuthorizationRequest authorizationRequest) {
        this.authorizationRequest = authorizationRequest;
    }

    @Override
    public String getQueryString() {
        QueryBuilder builder = QueryBuilder.instance();

        appendClientAuthnToQuery(builder);
        for (String key : getCustomParameters().keySet()) {
            builder.append(key, getCustomParameters().get(key));
        }

        return builder.toString();
    }

    public Integer getNbf() {
        return nbf;
    }

    public void setNbf(Integer nbf) {
        this.nbf = nbf;
    }
}