package io.jans.kc.spi.auth.oidc;

public interface OIDCUserInfoResponse {
    
    public String username();
    public String email();
    public boolean indicatesSuccess();
    public OIDCUserInfoError error();
}
