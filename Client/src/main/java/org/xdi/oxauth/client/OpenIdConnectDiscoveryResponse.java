package org.xdi.oxauth.client;

import org.xdi.oxauth.model.discovery.WebFingerLink;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Javier Rojas Blum Date: 01.28.2013
 */
public class OpenIdConnectDiscoveryResponse extends BaseResponse {

    private String subject;
    private List<WebFingerLink> links;

    /**
     * Constructs an OpenID Connect Discovery Response.
     *
     * @param status The response status code.
     */
    public OpenIdConnectDiscoveryResponse(int status) {
        super(status);
        links = new ArrayList<WebFingerLink>();
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<WebFingerLink> getLinks() {
        return links;
    }

    public void setLinks(List<WebFingerLink> links) {
        this.links = links;
    }
}