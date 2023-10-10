package io.jans.kc.spi.auth.oidc;

public interface OIDCMetaCache {
    public void put(String issuer, String key , Object value);
    public Object get(String issuer, String key);
}
