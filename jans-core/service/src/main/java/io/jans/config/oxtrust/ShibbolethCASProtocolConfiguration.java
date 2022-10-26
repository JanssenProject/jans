/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.oxtrust;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import java.io.Serializable;


/**
 * Shibboleth IDP CAS-related settings configuration entry.
 *
 * @author Dmitry Ognyannikov
 */
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShibbolethCASProtocolConfiguration implements Serializable {

    private static final long serialVersionUID = 1107303245739658346L;

    private String inum;

    private boolean enabled = true;

    private boolean extended = false;

    private boolean enableToProxyPatterns;

    private String authorizedToProxyPattern;

    private String unauthorizedToProxyPattern;

    private String sessionStorageType;

    /**
     * @return the inum
     */
    public String getInum() {
        return inum;
    }

    /**
     * @param inum
     *            the inum to set
     */
    public void setInum(String inum) {
        this.inum = inum;
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled
     *            the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the extended
     */
    public boolean isExtended() {
        return extended;
    }

    /**
     * @param extended
     *            the extended to set
     */
    public void setExtended(boolean extended) {
        this.extended = extended;
    }

    /**
     * @return the enableToProxyPatterns
     */
    public boolean isEnableToProxyPatterns() {
        return enableToProxyPatterns;
    }

    /**
     * @param enableToProxyPatterns
     *            the enableToProxyPatterns to set
     */
    public void setEnableToProxyPatterns(boolean enableToProxyPatterns) {
        this.enableToProxyPatterns = enableToProxyPatterns;
    }

    /**
     * @return the authorizedToProxyPattern
     */
    public String getAuthorizedToProxyPattern() {
        return authorizedToProxyPattern;
    }

    /**
     * @param authorizedToProxyPattern
     *            the authorizedToProxyPattern to set
     */
    public void setAuthorizedToProxyPattern(String authorizedToProxyPattern) {
        this.authorizedToProxyPattern = authorizedToProxyPattern;
    }

    /**
     * @return the unauthorizedToProxyPattern
     */
    public String getUnauthorizedToProxyPattern() {
        return unauthorizedToProxyPattern;
    }

    /**
     * @param unauthorizedToProxyPattern
     *            the unauthorizedToProxyPattern to set
     */
    public void setUnauthorizedToProxyPattern(String unauthorizedToProxyPattern) {
        this.unauthorizedToProxyPattern = unauthorizedToProxyPattern;
    }

    /**
     * @return the sessionStorageType
     */
    public String getSessionStorageType() {
        return sessionStorageType;
    }

    /**
     * @param sessionStorageType
     *            the sessionStorageType to set
     */
    public void setSessionStorageType(String sessionStorageType) {
        this.sessionStorageType = sessionStorageType;
    }

}
