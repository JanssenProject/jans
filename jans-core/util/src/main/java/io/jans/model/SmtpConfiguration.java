/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Transient;

/**
 * Hold SMTP configuration
 *
 * @author Yuriy Movchan Date: 04/20/2014
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmtpConfiguration implements java.io.Serializable {

    private static final long serialVersionUID = -5675038049444038755L;

    @JsonProperty("host")
    private String host = null;

    @JsonProperty("port")
    private int port = 0;
    
    @JsonProperty("connect_protection")
    private SmtpConnectProtectionType connectProtection = null;

    @JsonProperty("trust_host")
    private boolean serverTrust = false;

    @JsonProperty("from_name")
    private String fromName = null;

    @JsonProperty("from_email_address")
    private String fromEmailAddress = null;

    @JsonProperty("requires_authentication")
    private boolean requiresAuthentication = false;

    @JsonProperty("smtp_authentication_account_username")
    private String smtpAuthenticationAccountUsername = null;

    @JsonProperty("smtp_authentication_account_password")
    private String smtpAuthenticationAccountPassword = null;

    @Transient
    @JsonIgnore
    private String smtpAuthenticationAccountPasswordDecrypted = null;

    @JsonProperty("key_store")
    private String keyStore = null;

    @JsonProperty("key_store_password")
    private String keyStorePassword = null;

    @Transient
    @JsonIgnore
    private String keyStorePasswordDecrypted = null;

    @JsonProperty("key_store_alias")
    private String keyStoreAlias = null;

    @JsonProperty("signing_algorithm")
    private String signingAlgorithm = null;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    public SmtpConnectProtectionType getConnectProtection() {
        return connectProtection;
    }

    public void setConnectProtection(SmtpConnectProtectionType connectProtection) {
        this.connectProtection = connectProtection;
    }    

    public boolean isServerTrust() {
        return serverTrust;
    }

    public void setServerTrust(boolean serverTrust) {
        this.serverTrust = serverTrust;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getFromEmailAddress() {
        return fromEmailAddress;
    }

    public void setFromEmailAddress(String fromEmailAddress) {
        this.fromEmailAddress = fromEmailAddress;
    }

    public boolean isRequiresAuthentication() {
        return requiresAuthentication;
    }

    public void setRequiresAuthentication(boolean requiresAuthentication) {
        this.requiresAuthentication = requiresAuthentication;
    }

    public boolean isValid() {
        return getHost() != null && getPort() != 0
                && ((!isRequiresAuthentication())
                || (getSmtpAuthenticationAccountUsername() != null && getSmtpAuthenticationAccountPassword() != null));
    }

    public String getSmtpAuthenticationAccountUsername() {
        return smtpAuthenticationAccountUsername;
    }

    public void setSmtpAuthenticationAccountUsername(String smtpAuthenticationAccountUsername) {
        this.smtpAuthenticationAccountUsername = smtpAuthenticationAccountUsername;
    }

    public String getSmtpAuthenticationAccountPassword() {
        return smtpAuthenticationAccountPassword;
    }

    public void setSmtpAuthenticationAccountPassword(String smtpAuthenticationAccountPassword) {
        this.smtpAuthenticationAccountPassword = smtpAuthenticationAccountPassword;
    }

    public String getSmtpAuthenticationAccountPasswordDecrypted() {
        return smtpAuthenticationAccountPasswordDecrypted;
    }

    public void setSmtpAuthenticationAccountPasswordDecrypted(String smtpAuthenticationAccountPasswordDecrypted) {
        this.smtpAuthenticationAccountPasswordDecrypted = smtpAuthenticationAccountPasswordDecrypted;
    }
    
    public SmtpConnectProtectionType[] getConnectProtectionList() {
        return SmtpConnectProtectionType.values();
    }

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyStorePasswordDecrypted() {
        return keyStorePasswordDecrypted;
    }

    public void setKeyStorePasswordDecrypted(String keyStorePasswordDecrypted) {
        this.keyStorePasswordDecrypted = keyStorePasswordDecrypted;
    }

    public String getKeyStoreAlias() {
        return keyStoreAlias;
    }

    public void setKeyStoreAlias(String keyStoreAlias) {
        this.keyStoreAlias = keyStoreAlias;
    }

    public String getSigningAlgorithm() {
        return signingAlgorithm;
    }

    public void setSigningAlgorithm(String signingAlgorithm) {
        this.signingAlgorithm = signingAlgorithm;
    }

}
