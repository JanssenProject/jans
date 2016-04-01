package org.gluu.oxeleven.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version March 31, 2016
 */
public class JwksRequest extends BaseRequest {

    private List<String> aliasList;

    public JwksRequest() {
        setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        setMediaType(MediaType.APPLICATION_FORM_URLENCODED);
        setHttpMethod(HttpMethod.POST);
    }

    public List<String> getAliasList() {
        return aliasList;
    }

    public void setAliasList(List<String> aliasList) {
        this.aliasList = aliasList;
    }
}
