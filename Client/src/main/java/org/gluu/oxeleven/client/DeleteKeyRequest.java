package org.gluu.oxeleven.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

/**
 * @author Javier Rojas Blum
 * @version March 29, 2016
 */
public class DeleteKeyRequest extends BaseRequest {

    private String alias;

    public DeleteKeyRequest() {
        setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        setMediaType(MediaType.APPLICATION_FORM_URLENCODED);
        setHttpMethod(HttpMethod.POST);
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}
