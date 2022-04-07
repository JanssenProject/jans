/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.discovery;

/**
 * @author Javier Rojas Blum Date: 09.16.2013
 */
public class WebFingerParam {
    private WebFingerParam() {
    }

    /**
     * Identifier of the target End-User that is the subject of the discovery request.
     */
    public static final String RESOURCE = "resource";

    /**
     * Server where a WebFinger service is hosted.
     */
    public static final String HOST = "host";

    /**
     * URI identifying the type of service whose location is requested.
     */
    public static final String REL = "rel";

    public static final String REL_VALUE = "http://openid.net/specs/connect/1.0/issuer";

    public static final String SUBJECT = "subject";
    public static final String LINKS = "links";
    public static final String HREF = "href";
}