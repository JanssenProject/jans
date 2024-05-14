/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config;

import java.io.Serializable;
import java.util.List;

import io.jans.model.SmtpConfiguration;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.InumEntry;
import io.jans.service.cache.CacheConfiguration;
import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.service.message.model.config.MessageConfiguration;

/**
 * Gluu Configuration
 *
 * @author Yuriy Movchan Date: 08.27.2012
 */
@DataEntry
@ObjectClass(value = "jansAppConf")
public class GluuConfiguration extends InumEntry implements Serializable {

    private static final long serialVersionUID = -2818003894646725601L;

    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "jansSmtpConf")
    @JsonObject
    private SmtpConfiguration smtpConfiguration;

    @AttributeName(name = "jansCacheConf")
    @JsonObject
    private CacheConfiguration cacheConfiguration;

    @AttributeName(name = "jansMessageConf")
    @JsonObject
    private MessageConfiguration messageConfiguration;

    @AttributeName(name = "jansDocStoreConf")
    @JsonObject
    private DocumentStoreConfiguration documentStoreConfiguration;

    @AttributeName(name = "jansDbAuth")
    @JsonObject
    private List<IDPAuthConf> idpAuthn;

    @AttributeName(name = "jansAuthMode")
    private String authenticationMode;

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public SmtpConfiguration getSmtpConfiguration() {
        return smtpConfiguration;
    }

    public void setSmtpConfiguration(SmtpConfiguration smtpConfiguration) {
        this.smtpConfiguration = smtpConfiguration;
    }

    public CacheConfiguration getCacheConfiguration() {
        return cacheConfiguration;
    }

    public void setCacheConfiguration(CacheConfiguration cacheConfiguration) {
        this.cacheConfiguration = cacheConfiguration;
    }

    public MessageConfiguration getMessageConfiguration() {
		return messageConfiguration;
	}

	public void setMessageConfiguration(MessageConfiguration messageConfiguration) {
		this.messageConfiguration = messageConfiguration;
	}

	public DocumentStoreConfiguration getDocumentStoreConfiguration() {
        return documentStoreConfiguration;
    }

    public void setDocumentStoreConfiguration(DocumentStoreConfiguration documentStoreConfiguration) {
        this.documentStoreConfiguration = documentStoreConfiguration;
    }

    public List<IDPAuthConf> getIdpAuthn() {
        return idpAuthn;
    }

    public void setIdpAuthn(List<IDPAuthConf> idpAuthn) {
        this.idpAuthn = idpAuthn;
    }

    public String getAuthenticationMode() {
        return authenticationMode;
    }

    public void setAuthenticationMode(String authenticationMode) {
        this.authenticationMode = authenticationMode;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GluuConfiguration [inum=").append(inum).append(", smtpConfiguration=").append(smtpConfiguration).append(", idpAuthn=")
                .append(idpAuthn).append(", authenticationMode=").append(authenticationMode).append(", toString()=").append(super.toString())
                .append("]");
        return builder.toString();
    }

}
