package org.gluu.oxeleven.client;

/**
 * @author Javier Rojas Blum
 * @version March 29, 2016
 */
public abstract class BaseRequest {

    private String contentType;
    private String mediaType;
    private String httpMethod;

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
}
