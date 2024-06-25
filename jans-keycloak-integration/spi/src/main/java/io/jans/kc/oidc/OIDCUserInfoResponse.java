package io.jans.kc.oidc;

public interface OIDCUserInfoResponse {
    
    public String username();
    public String email();
    public boolean indicatesSuccess();
    public OIDCUserInfoError error();
}
