package org.oxauth.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientAttributes implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 213428216912083393L;

    @JsonProperty("tlsClientAuthSubjectDn")
    private String tlsClientAuthSubjectDn;

    @JsonProperty("runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims")
    private Boolean runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims = false;

    @JsonProperty("keepClientAuthorizationAfterExpiration")
    private Boolean keepClientAuthorizationAfterExpiration = false;

    @JsonProperty("allowSpontaneousScopes")
    private Boolean allowSpontaneousScopes = false;

    @JsonProperty("spontaneousScopes")
    private List<String> spontaneousScopes = Lists.newArrayList();

    @JsonProperty("spontaneousScopeScriptDns")
    private List<String> spontaneousScopeScriptDns = Lists.newArrayList();


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

    public Boolean getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims() {
        if (runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims == null) {
            runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims = false;
        }
        return runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims;
    }

    public void setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(Boolean runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims) {
        this.runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims = runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims;
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

    @Override
    public String toString() {
        return "ClientAttributes{" +
                "tlsClientAuthSubjectDn='" + tlsClientAuthSubjectDn + '\'' +
                ", runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims=" + runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims +
                ", keepClientAuthorizationAfterExpiration=" + keepClientAuthorizationAfterExpiration +
                ", allowSpontaneousScopes=" + allowSpontaneousScopes +
                ", spontaneousScopes=" + spontaneousScopes +
                ", spontaneousScopeScriptDns=" + spontaneousScopeScriptDns +
                '}';
    }
}
