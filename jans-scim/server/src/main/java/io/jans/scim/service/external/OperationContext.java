package io.jans.scim.service.external;

import java.util.Map;
import java.net.URI;
import jakarta.ws.rs.core.MultivaluedMap;

public class OperationContext {

    private String path;
    private URI baseUri;
    private String method;
    private String resourceType;
    private MultivaluedMap<String, String> queryParams;
    private MultivaluedMap<String, String> requestHeaders;
    private TokenDetails tokenDetails;
    private String filterPrepend;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }

    public MultivaluedMap<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(MultivaluedMap<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public MultivaluedMap<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(MultivaluedMap<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public TokenDetails getTokenDetails() {
        return tokenDetails;
    }

    public void setTokenDetails(TokenDetails tokenDetails) {
        this.tokenDetails = tokenDetails;
    }
    public String getFilterPrepend() {
        return filterPrepend;
    }

    public void setFilterPrepend(String filterPrepend) {
        this.filterPrepend = filterPrepend;
    }

}
