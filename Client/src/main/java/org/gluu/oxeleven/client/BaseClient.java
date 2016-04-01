package org.gluu.oxeleven.client;

import com.google.common.base.Strings;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;

import javax.ws.rs.HttpMethod;

/**
 * @author Javier Rojas Blum
 * @version March 31, 2016
 */
public abstract class BaseClient<T extends BaseRequest, V extends BaseResponse> {

    protected String url;
    protected BaseRequest request;
    protected BaseResponse response;
    protected ClientRequest clientRequest;
    protected ClientResponse<String> clientResponse;

    public BaseClient(String url) {
        this.url = url;
    }

    protected void addRequestParam(String key, String value) {
        if (!Strings.isNullOrEmpty(key) && !Strings.isNullOrEmpty(value)) {
            if (HttpMethod.POST.equals(request.getHttpMethod())) {
                clientRequest.formParameter(key, value);
            } else {
                clientRequest.queryParameter(key, value);
            }
        }
    }

    protected void addRequestParam(String key, String[] value) {
        if (!Strings.isNullOrEmpty(key) && value != null) {
            if (HttpMethod.POST.equals(request.getHttpMethod())) {
                clientRequest.formParameter(key, value);
            } else {
                clientRequest.queryParameter(key, value);
            }
        }
    }

    public abstract T getRequest();

    public abstract void setRequest(T request);

    public abstract V getResponse();

    public abstract void setResponse(V response);

    public abstract V exec() throws Exception;
}
