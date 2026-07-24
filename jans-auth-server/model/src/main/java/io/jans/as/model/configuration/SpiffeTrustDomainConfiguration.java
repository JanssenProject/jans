package io.jans.as.model.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Admin-configured, out-of-band trust anchor mapping for a single SPIFFE trust domain, used by
 * SPIFFE-based client authentication (draft-ietf-oauth-spiffe-client-auth). The bundle endpoint
 * is deliberately configured here rather than trusted from client-supplied metadata
 * (`spiffe_bundle_endpoint`), since trusting a client-supplied URL for trust-anchor material
 * would let a client vouch for itself.
 *
 * @author Yuriy Zabrovarnyy
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpiffeTrustDomainConfiguration {

    public static final int DEFAULT_BUNDLE_CACHE_LIFETIME_IN_MINUTES = 60;

    private String trustDomain;
    private String bundleEndpointUrl;
    private Integer bundleCacheLifetimeInMinutes = DEFAULT_BUNDLE_CACHE_LIFETIME_IN_MINUTES;

    public SpiffeTrustDomainConfiguration() {
    }

    @JsonCreator
    public SpiffeTrustDomainConfiguration(
            @JsonProperty("trustDomain") String trustDomain,
            @JsonProperty("bundleEndpointUrl") String bundleEndpointUrl,
            @JsonProperty("bundleCacheLifetimeInMinutes") Integer bundleCacheLifetimeInMinutes) {
        this.trustDomain = trustDomain;
        this.bundleEndpointUrl = bundleEndpointUrl;
        setBundleCacheLifetimeInMinutes(bundleCacheLifetimeInMinutes);
    }

    @JsonProperty("trustDomain")
    public String getTrustDomain() {
        return trustDomain;
    }

    @JsonProperty("trustDomain")
    public void setTrustDomain(String trustDomain) {
        this.trustDomain = trustDomain;
    }

    @JsonProperty("bundleEndpointUrl")
    public String getBundleEndpointUrl() {
        return bundleEndpointUrl;
    }

    @JsonProperty("bundleEndpointUrl")
    public void setBundleEndpointUrl(String bundleEndpointUrl) {
        this.bundleEndpointUrl = bundleEndpointUrl;
    }

    @JsonProperty("bundleCacheLifetimeInMinutes")
    public Integer getBundleCacheLifetimeInMinutes() {
        return bundleCacheLifetimeInMinutes;
    }

    @JsonProperty("bundleCacheLifetimeInMinutes")
    public void setBundleCacheLifetimeInMinutes(Integer bundleCacheLifetimeInMinutes) {
        this.bundleCacheLifetimeInMinutes = (bundleCacheLifetimeInMinutes != null && bundleCacheLifetimeInMinutes > 0)
                ? bundleCacheLifetimeInMinutes
                : DEFAULT_BUNDLE_CACHE_LIFETIME_IN_MINUTES;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpiffeTrustDomainConfiguration)) return false;
        SpiffeTrustDomainConfiguration that = (SpiffeTrustDomainConfiguration) o;
        return Objects.equals(trustDomain, that.trustDomain)
                && Objects.equals(bundleEndpointUrl, that.bundleEndpointUrl)
                && Objects.equals(bundleCacheLifetimeInMinutes, that.bundleCacheLifetimeInMinutes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trustDomain, bundleEndpointUrl, bundleCacheLifetimeInMinutes);
    }

    @Override
    public String toString() {
        return "SpiffeTrustDomainConfiguration{" +
                "trustDomain='" + trustDomain + '\'' +
                ", bundleEndpointUrl='" + bundleEndpointUrl + '\'' +
                ", bundleCacheLifetimeInMinutes=" + bundleCacheLifetimeInMinutes +
                '}';
    }
}
