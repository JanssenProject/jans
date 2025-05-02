/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version March 17, 2022
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientAttributes implements Serializable {

    public static final int DEFAULT_PAR_LIFETIME = 600;

    /**
     *
     */
    private static final long serialVersionUID = 213428216912083395L;

    @JsonProperty("tlsClientAuthSubjectDn")
    private String tlsClientAuthSubjectDn;

    @JsonProperty("runIntrospectionScriptBeforeJwtCreation")
    private Boolean runIntrospectionScriptBeforeJwtCreation = false;

    @JsonProperty("keepClientAuthorizationAfterExpiration")
    private Boolean keepClientAuthorizationAfterExpiration = false;

    @JsonProperty("allowSpontaneousScopes")
    private Boolean allowSpontaneousScopes = false;

    @JsonProperty("spontaneousScopes")
    private List<String> spontaneousScopes = Lists.newArrayList();

    @JsonProperty("spontaneousScopeScriptDns")
    private List<String> spontaneousScopeScriptDns = Lists.newArrayList();

    @JsonProperty("updateTokenScriptDns")
    private List<String> updateTokenScriptDns = Lists.newArrayList();

    @JsonProperty("backchannelLogoutUri")
    private List<String> backchannelLogoutUri;

    @JsonProperty("backchannelLogoutSessionRequired")
    private Boolean backchannelLogoutSessionRequired;

    @JsonProperty("additionalAudience")
    private List<String> additionalAudience;

    @JsonProperty("postAuthnScripts")
    private List<String> postAuthnScripts;

    @JsonProperty("tokenExchangeScripts")
    private List<String> tokenExchangeScripts;

    @JsonProperty("consentGatheringScripts")
    private List<String> consentGatheringScripts;

    @JsonProperty("introspectionScripts")
    private List<String> introspectionScripts;

    @JsonProperty("rptClaimsScripts")
    private List<String> rptClaimsScripts;

    @JsonProperty("ropcScripts")
    private List<String> ropcScripts;

    @JsonProperty("parLifetime")
    private Integer parLifetime;

    @JsonProperty("requirePar")
    private Boolean requirePar;

    @JsonProperty("dpopBoundAccessToken")
    private Boolean dpopBoundAccessToken;

    @JsonProperty("jansAuthSignedRespAlg")
    private String authorizationSignedResponseAlg;

    @JsonProperty("jansAuthEncRespAlg")
    private String authorizationEncryptedResponseAlg;

    @JsonProperty("jansAuthEncRespEnc")
    private String authorizationEncryptedResponseEnc;

    @JsonProperty("jansSubAttr")
    private String publicSubjectIdentifierAttribute;

    @JsonProperty("redirectUrisRegex")
    private String redirectUrisRegex;

    @JsonProperty("jansAuthorizedAcr")
    private List<String> authorizedAcrValues;

    @JsonProperty("jansDefaultPromptLogin")
    private Boolean defaultPromptLogin = false;

    @JsonProperty("txTokenLifetime")
    private Integer txTokenLifetime;

    @JsonProperty("idTokenLifetime")
    private Integer idTokenLifetime;

    @JsonProperty("allowOfflineAccessWithoutConsent")
    private Boolean allowOfflineAccessWithoutConsent;

    @JsonProperty("requirePkce")
    private Boolean requirePkce = false;

    @JsonProperty("minimumAcrLevel")
    private Integer minimumAcrLevel = -1;

    @JsonProperty("minimumAcrLevelAutoresolve")
    private Boolean minimumAcrLevelAutoresolve;

    @JsonProperty("additionalTokenEndpointAuthMethods")
    private List<String> additionalTokenEndpointAuthMethods;

    @JsonProperty("minimumAcrPriorityList")
    private List<String> minimumAcrPriorityList;

    @JsonProperty("requestedLifetime")
    private Integer requestedLifetime; // in seconds

    @JsonProperty("evidence")
    private String evidence;

    @JsonProperty("introspectionSignedResponseAlg")
    private String introspectionSignedResponseAlg;

    @JsonProperty("introspectionEncryptedResponseAlg")
    private String introspectionEncryptedResponseAlg;

    @JsonProperty("introspectionEncryptedResponseEnc")
    private String introspectionEncryptedResponseEnc;

    @JsonProperty("txTokenSignedResponseAlg")
    private String txTokenSignedResponseAlg;

    @JsonProperty("txTokenEncryptedResponseAlg")
    private String txTokenEncryptedResponseAlg;

    @JsonProperty("txTokenEncryptedResponseEnc")
    private String txTokenEncryptedResponseEnc;

    @JsonProperty("authorizationDetailsTypes")
    private List<String> authorizationDetailsTypes;

    public Boolean getRequirePkce() {
        if (requirePkce == null) {
            requirePkce = false;
        }
        return requirePkce;
    }

    public void setRequirePkce(Boolean requirePkce) {
        this.requirePkce = requirePkce;
    }

    public List<String> getAuthorizationDetailsTypes() {
        if (authorizationDetailsTypes == null) authorizationDetailsTypes = new ArrayList<>();
        return authorizationDetailsTypes;
    }

    public void setAuthorizationDetailsTypes(List<String> authorizationDetailsTypes) {
        this.authorizationDetailsTypes = authorizationDetailsTypes;
    }

    public String getTxTokenSignedResponseAlg() {
        return txTokenSignedResponseAlg;
    }

    public void setTxTokenSignedResponseAlg(String txTokenSignedResponseAlg) {
        this.txTokenSignedResponseAlg = txTokenSignedResponseAlg;
    }

    public String getTxTokenEncryptedResponseAlg() {
        return txTokenEncryptedResponseAlg;
    }

    public void setTxTokenEncryptedResponseAlg(String txTokenEncryptedResponseAlg) {
        this.txTokenEncryptedResponseAlg = txTokenEncryptedResponseAlg;
    }

    public String getTxTokenEncryptedResponseEnc() {
        return txTokenEncryptedResponseEnc;
    }

    public void setTxTokenEncryptedResponseEnc(String txTokenEncryptedResponseEnc) {
        this.txTokenEncryptedResponseEnc = txTokenEncryptedResponseEnc;
    }

    public String getIntrospectionSignedResponseAlg() {
        return introspectionSignedResponseAlg;
    }

    public void setIntrospectionSignedResponseAlg(String introspectionSignedResponseAlg) {
        this.introspectionSignedResponseAlg = introspectionSignedResponseAlg;
    }

    public String getIntrospectionEncryptedResponseAlg() {
        return introspectionEncryptedResponseAlg;
    }

    public void setIntrospectionEncryptedResponseAlg(String introspectionEncryptedResponseAlg) {
        this.introspectionEncryptedResponseAlg = introspectionEncryptedResponseAlg;
    }

    public String getIntrospectionEncryptedResponseEnc() {
        return introspectionEncryptedResponseEnc;
    }

    public void setIntrospectionEncryptedResponseEnc(String introspectionEncryptedResponseEnc) {
        this.introspectionEncryptedResponseEnc = introspectionEncryptedResponseEnc;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public Boolean getDpopBoundAccessToken() {
        if (dpopBoundAccessToken == null) dpopBoundAccessToken = false;
        return dpopBoundAccessToken;
    }

    public void setDpopBoundAccessToken(Boolean dpopBoundAccessToken) {
        this.dpopBoundAccessToken = dpopBoundAccessToken;
    }

    public Integer getRequestedLifetime() {
        return requestedLifetime;
    }

    public void setRequestedLifetime(Integer requestedLifetime) {
        this.requestedLifetime = requestedLifetime;
    }

    public Boolean getMinimumAcrLevelAutoresolve() {
        return minimumAcrLevelAutoresolve;
    }

    public void setMinimumAcrLevelAutoresolve(Boolean minimumAcrLevelAutoresolve) {
        this.minimumAcrLevelAutoresolve = minimumAcrLevelAutoresolve;
    }

    public List<String> getAdditionalTokenEndpointAuthMethods() {
        if (additionalTokenEndpointAuthMethods == null) additionalTokenEndpointAuthMethods = new ArrayList<>();
        return additionalTokenEndpointAuthMethods;
    }

    public void setAdditionalTokenEndpointAuthMethods(List<String> additionalTokenEndpointAuthMethods) {
        this.additionalTokenEndpointAuthMethods = additionalTokenEndpointAuthMethods;
    }

    public List<String> getMinimumAcrPriorityList() {
        if (minimumAcrPriorityList == null) minimumAcrPriorityList = new ArrayList<>();
        return minimumAcrPriorityList;
    }

    public void setMinimumAcrPriorityList(List<String> minimumAcrPriorityList) {
        this.minimumAcrPriorityList = minimumAcrPriorityList;
    }

    public Integer getMinimumAcrLevel() {
        if (minimumAcrLevel == null) minimumAcrLevel = -1;
        return minimumAcrLevel;
    }

    public void setMinimumAcrLevel(Integer minimumAcrLevel) {
        this.minimumAcrLevel = minimumAcrLevel;
    }

    public Boolean getAllowOfflineAccessWithoutConsent() {
        return allowOfflineAccessWithoutConsent;
    }

    public void setAllowOfflineAccessWithoutConsent(Boolean allowOfflineAccessWithoutConsent) {
        this.allowOfflineAccessWithoutConsent = allowOfflineAccessWithoutConsent;
    }

    public Integer getTxTokenLifetime() {
        return txTokenLifetime;
    }

    public void setTxTokenLifetime(Integer txTokenLifetime) {
        this.txTokenLifetime = txTokenLifetime;
    }

    public Integer getIdTokenLifetime() {
        return idTokenLifetime;
    }

    public void setIdTokenLifetime(Integer idTokenLifetime) {
        this.idTokenLifetime = idTokenLifetime;
    }

    public List<String> getRopcScripts() {
        if (ropcScripts == null) ropcScripts = new ArrayList<>();
        return ropcScripts;
    }

    public void setRopcScripts(List<String> ropcScripts) {
        this.ropcScripts = ropcScripts;
    }

    public Boolean getRequirePar() {
        if (requirePar == null) requirePar = false;
        return requirePar;
    }

    public void setRequirePar(Boolean requirePar) {
        this.requirePar = requirePar;
    }

    public Integer getParLifetime() {
        if (parLifetime == null || parLifetime == 0)
            parLifetime = DEFAULT_PAR_LIFETIME;
        return parLifetime;
    }

    public void setParLifetime(Integer parLifetime) {
        this.parLifetime = parLifetime;
    }

    public List<String> getRptClaimsScripts() {
        if (rptClaimsScripts == null) rptClaimsScripts = Lists.newArrayList();
        return rptClaimsScripts;
    }

    public void setRptClaimsScripts(List<String> rptClaimsScripts) {
        this.rptClaimsScripts = rptClaimsScripts;
    }

    public List<String> getIntrospectionScripts() {
        if (introspectionScripts == null) introspectionScripts = Lists.newArrayList();
        return introspectionScripts;
    }

    public void setIntrospectionScripts(List<String> introspectionScripts) {
        this.introspectionScripts = introspectionScripts;
    }

    public List<String> getPostAuthnScripts() {
        if (postAuthnScripts == null) postAuthnScripts = Lists.newArrayList();
        return postAuthnScripts;
    }

    public void setPostAuthnScripts(List<String> postAuthnScripts) {
        this.postAuthnScripts = postAuthnScripts;
    }

    public List<String> getTokenExchangeScripts() {
        if (tokenExchangeScripts == null) tokenExchangeScripts = Lists.newArrayList();
        return tokenExchangeScripts;
    }

    public ClientAttributes setTokenExchangeScripts(List<String> tokenExchangeScripts) {
        this.tokenExchangeScripts = tokenExchangeScripts;
        return this;
    }

    public List<String> getConsentGatheringScripts() {
        if (consentGatheringScripts == null) consentGatheringScripts = Lists.newArrayList();
        return consentGatheringScripts;
    }

    public void setConsentGatheringScripts(List<String> consentGatheringScripts) {
        this.consentGatheringScripts = consentGatheringScripts;
    }

    public List<String> getAdditionalAudience() {
        if (additionalAudience == null) additionalAudience = Lists.newArrayList();
        return additionalAudience;
    }

    public void setAdditionalAudience(List<String> additionalAudience) {
        this.additionalAudience = additionalAudience;
    }

    public String getTlsClientAuthSubjectDn() {
        return tlsClientAuthSubjectDn;
    }

    public void setTlsClientAuthSubjectDn(String tlsClientAuthSubjectDn) {
        this.tlsClientAuthSubjectDn = tlsClientAuthSubjectDn;
    }

    public Boolean getAllowSpontaneousScopes() {
        if (allowSpontaneousScopes == null) allowSpontaneousScopes = false;
        return allowSpontaneousScopes;
    }

    public void setAllowSpontaneousScopes(Boolean allowSpontaneousScopes) {
        this.allowSpontaneousScopes = allowSpontaneousScopes;
    }

    public List<String> getSpontaneousScopes() {
        if (spontaneousScopes == null) spontaneousScopes = Lists.newArrayList();
        return spontaneousScopes;
    }

    public void setSpontaneousScopes(List<String> spontaneousScopes) {
        this.spontaneousScopes = spontaneousScopes;
    }

    public List<String> getSpontaneousScopeScriptDns() {
        if (spontaneousScopeScriptDns == null) spontaneousScopeScriptDns = Lists.newArrayList();
        return spontaneousScopeScriptDns;
    }

    public void setSpontaneousScopeScriptDns(List<String> spontaneousScopeScriptDns) {
        this.spontaneousScopeScriptDns = spontaneousScopeScriptDns;
    }

    public List<String> getUpdateTokenScriptDns() {
        if (updateTokenScriptDns == null) updateTokenScriptDns = Lists.newArrayList();
        return updateTokenScriptDns;
    }

    public void setUpdateTokenScriptDns(List<String> updateTokenScriptDns) {
        this.updateTokenScriptDns = updateTokenScriptDns;
    }

    public Boolean getRunIntrospectionScriptBeforeJwtCreation() {
        if (runIntrospectionScriptBeforeJwtCreation == null) {
            runIntrospectionScriptBeforeJwtCreation = false;
        }
        return runIntrospectionScriptBeforeJwtCreation;
    }

    public void setRunIntrospectionScriptBeforeJwtCreation(Boolean runIntrospectionScriptBeforeJwtCreation) {
        this.runIntrospectionScriptBeforeJwtCreation = runIntrospectionScriptBeforeJwtCreation;
    }

    public Boolean getKeepClientAuthorizationAfterExpiration() {
        if (keepClientAuthorizationAfterExpiration == null) {
            keepClientAuthorizationAfterExpiration = false;
        }
        return keepClientAuthorizationAfterExpiration;
    }

    public void setKeepClientAuthorizationAfterExpiration(Boolean keepClientAuthorizationAfterExpiration) {
        this.keepClientAuthorizationAfterExpiration = keepClientAuthorizationAfterExpiration;
    }

    public List<String> getBackchannelLogoutUri() {
        if (backchannelLogoutUri == null) backchannelLogoutUri = Lists.newArrayList();
        return backchannelLogoutUri;
    }

    public void setBackchannelLogoutUri(List<String> backchannelLogoutUri) {
        this.backchannelLogoutUri = backchannelLogoutUri;
    }

    public Boolean getBackchannelLogoutSessionRequired() {
        if (backchannelLogoutSessionRequired == null) backchannelLogoutSessionRequired = false;
        return backchannelLogoutSessionRequired;
    }

    public void setBackchannelLogoutSessionRequired(Boolean backchannelLogoutSessionRequired) {
        this.backchannelLogoutSessionRequired = backchannelLogoutSessionRequired;
    }

    public String getAuthorizationSignedResponseAlg() {
        return authorizationSignedResponseAlg;
    }

    public void setAuthorizationSignedResponseAlg(String authorizationSignedResponseAlg) {
        this.authorizationSignedResponseAlg = authorizationSignedResponseAlg;
    }

    public String getAuthorizationEncryptedResponseAlg() {
        return authorizationEncryptedResponseAlg;
    }

    public void setAuthorizationEncryptedResponseAlg(String authorizationEncryptedResponseAlg) {
        this.authorizationEncryptedResponseAlg = authorizationEncryptedResponseAlg;
    }

    public String getAuthorizationEncryptedResponseEnc() {
        return authorizationEncryptedResponseEnc;
    }

    public void setAuthorizationEncryptedResponseEnc(String authorizationEncryptedResponseEnc) {
        this.authorizationEncryptedResponseEnc = authorizationEncryptedResponseEnc;
    }

    /**
     * Return the custom subject identifier attribute. It is used for public subject type.
     * If null, the default is used. Else use the custom attribute per client basis.
     *
     * @return The custom subject identifier attribute.
     */
    public String getPublicSubjectIdentifierAttribute() {
        return publicSubjectIdentifierAttribute;
    }

    /**
     * Sets the custom subject identifier attribute. It is used for public subject type.
     * if null, the default is used. Else use the custom attribute per client basis.
     *
     * @param publicSubjectIdentifierAttribute The custom subject identifier attribute.
     */
    public void setPublicSubjectIdentifierAttribute(String publicSubjectIdentifierAttribute) {
        this.publicSubjectIdentifierAttribute = publicSubjectIdentifierAttribute;
    }

    public String getRedirectUrisRegex() {
        return redirectUrisRegex;
    }

    public void setRedirectUrisRegex(String redirectUrisRegex) {
        this.redirectUrisRegex = redirectUrisRegex;
    }

    public List<String> getAuthorizedAcrValues() {
        if (authorizedAcrValues == null) {
            return Lists.newArrayList();
        }
        return authorizedAcrValues;
    }

    public void setAuthorizedAcrValues(List<String> authorizedAcrValues) {
        this.authorizedAcrValues = authorizedAcrValues;
    }

    public Boolean getDefaultPromptLogin() {
        if (defaultPromptLogin == null) {
            defaultPromptLogin = false;
        }

        return defaultPromptLogin;
    }

    public void setDefaultPromptLogin(Boolean defaultPromptLogin) {
        this.defaultPromptLogin = defaultPromptLogin;
    }

    @Override
    public String toString() {
        return "ClientAttributes{" +
                "tlsClientAuthSubjectDn='" + tlsClientAuthSubjectDn + '\'' +
                ", runIntrospectionScriptBeforeJwtCreation=" + runIntrospectionScriptBeforeJwtCreation +
                ", keepClientAuthorizationAfterExpiration=" + keepClientAuthorizationAfterExpiration +
                ", allowSpontaneousScopes=" + allowSpontaneousScopes +
                ", spontaneousScopes=" + spontaneousScopes +
                ", spontaneousScopeScriptDns=" + spontaneousScopeScriptDns +
                ", updateTokenScriptDns=" + updateTokenScriptDns +
                ", backchannelLogoutUri=" + backchannelLogoutUri +
                ", backchannelLogoutSessionRequired=" + backchannelLogoutSessionRequired +
                ", additionalAudience=" + additionalAudience +
                ", postAuthnScripts=" + postAuthnScripts +
                ", tokenExchangeScripts=" + tokenExchangeScripts +
                ", consentGatheringScripts=" + consentGatheringScripts +
                ", introspectionScripts=" + introspectionScripts +
                ", rptClaimsScripts=" + rptClaimsScripts +
                ", authorizationSignedResponseAlg=" + authorizationSignedResponseAlg +
                ", authorizationEncryptedResponseAlg=" + authorizationEncryptedResponseAlg +
                ", authorizationEncryptedResponseEnc=" + authorizationEncryptedResponseEnc +
                ", publicSubjectIdentifierAttribute=" + publicSubjectIdentifierAttribute +
                ", redirectUrisRegex=" + redirectUrisRegex +
                ", allowOfflineAccessWithoutConsent=" + allowOfflineAccessWithoutConsent +
                ", minimumAcrLevel=" + minimumAcrLevel +
                ", minimumAcrLevelAutoresolve=" + minimumAcrLevelAutoresolve +
                ", additionalTokenEndpointAuthMethods=" + additionalTokenEndpointAuthMethods +
                ", minimumAcrPriorityList=" + minimumAcrPriorityList +
                ", defaultPromptLogin=" + defaultPromptLogin +
                ", requestedLifetime=" + requestedLifetime +
                ", requirePar=" + requirePar +
                ", evidence=" + evidence +
                ", idTokenLifetime=" + idTokenLifetime +
                ", txTokenLifetime=" + txTokenLifetime +
                ", dpopBoundAccessToken=" + dpopBoundAccessToken +
                ", introspectionSignedResponseAlg=" + introspectionSignedResponseAlg +
                ", introspectionEncryptedResponseAlg=" + introspectionEncryptedResponseAlg +
                ", introspectionEncryptedResponseEnc=" + introspectionEncryptedResponseEnc +
                ", txTokenSignedResponseAlg=" + txTokenSignedResponseAlg +
                ", txTokenEncryptedResponseAlg=" + txTokenEncryptedResponseAlg +
                ", txTokenEncryptedResponseEnc=" + txTokenEncryptedResponseEnc +
                ", authorizationDetailsTypes=" + authorizationDetailsTypes +
                '}';
    }
}
