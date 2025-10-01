/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.discovery.WebFingerLink;

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
        links = new ArrayList<>();
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