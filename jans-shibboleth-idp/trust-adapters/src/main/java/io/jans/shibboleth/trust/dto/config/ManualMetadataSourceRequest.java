package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Metadata described by hand for a single entity (`type: MANUAL`). Valid for INDIVIDUAL trust
 * relationships only. {@code valid_until} is an ISO-8601 date-time; {@code signing_certificate} is
 * an optional base64 certificate string (omitted means no signing certificate). No files are
 * involved.
 */
public class ManualMetadataSourceRequest extends MetadataSourceRequest {

    @JsonProperty("entity_id")
    private String entityId;

    @JsonProperty("valid_until")
    private String validUntil;

    @JsonProperty("assertion_consumer_service")
    private AssertionConsumerServiceRequest assertionConsumerService;

    @JsonProperty("signing_certificate")
    private String signingCertificate;

    public ManualMetadataSourceRequest() {
    }

    public String getEntityId() {

        return entityId;
    }

    public void setEntityId(String entityId) {

        this.entityId = entityId;
    }

    public String getValidUntil() {

        return validUntil;
    }

    public void setValidUntil(String validUntil) {

        this.validUntil = validUntil;
    }

    public AssertionConsumerServiceRequest getAssertionConsumerService() {

        return assertionConsumerService;
    }

    public void setAssertionConsumerService(AssertionConsumerServiceRequest assertionConsumerService) {

        this.assertionConsumerService = assertionConsumerService;
    }

    public String getSigningCertificate() {

        return signingCertificate;
    }

    public void setSigningCertificate(String signingCertificate) {

        this.signingCertificate = signingCertificate;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ManualMetadataSourceRequest that = (ManualMetadataSourceRequest) o;
        return Objects.equals(entityId, that.entityId)
            && Objects.equals(validUntil, that.validUntil)
            && Objects.equals(assertionConsumerService, that.assertionConsumerService)
            && Objects.equals(signingCertificate, that.signingCertificate);
    }

    @Override
    public int hashCode() {

        return Objects.hash(entityId, validUntil, assertionConsumerService, signingCertificate);
    }

    @Override
    public String toString() {

        return "ManualMetadataSourceRequest{entityId='" + entityId + "', validUntil='" + validUntil
            + "', assertionConsumerService=" + assertionConsumerService
            + ", signingCertificate=" + (signingCertificate == null ? "null" : "<redacted>") + '}';
    }
}
