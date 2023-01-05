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
 * Asimba settings configuration entry.
 *
 * @author Dmitry Ognyannikov
 */
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AsimbaConfiguration implements Serializable {

    private static final long serialVersionUID = -1207383545739619576L;

    private String orgInum;

    private String oxasimba;

    private String idps;

    private String selectors;

    private String requestors;

    private String requestorpools;

    /**
     * @return the orgInum
     */
    public String getOrgInum() {
        return orgInum;
    }

    /**
     * @param orgInum
     *            the orgInum to set
     */
    public void setOrgInum(String orgInum) {
        this.orgInum = orgInum;
    }

    /**
     * @return the oxasimba
     */
    public String getOxasimba() {
        return oxasimba;
    }

    /**
     * @param oxasimba
     *            the oxasimba to set
     */
    public void setOxasimba(String oxasimba) {
        this.oxasimba = oxasimba;
    }

    /**
     * @return the idps
     */
    public String getIdps() {
        return idps;
    }

    /**
     * @param idps
     *            the idps to set
     */
    public void setIdps(String idps) {
        this.idps = idps;
    }

    /**
     * @return the selectors
     */
    public String getSelectors() {
        return selectors;
    }

    /**
     * @param selectors
     *            the selectors to set
     */
    public void setSelectors(String selectors) {
        this.selectors = selectors;
    }

    /**
     * @return the requestors
     */
    public String getRequestors() {
        return requestors;
    }

    /**
     * @param requestors
     *            the requestors to set
     */
    public void setRequestors(String requestors) {
        this.requestors = requestors;
    }

    /**
     * @return the requestorpools
     */
    public String getRequestorpools() {
        return requestorpools;
    }

    /**
     * @param requestorpools
     *            the requestorpools to set
     */
    public void setRequestorpools(String requestorpools) {
        this.requestorpools = requestorpools;
    }

}
