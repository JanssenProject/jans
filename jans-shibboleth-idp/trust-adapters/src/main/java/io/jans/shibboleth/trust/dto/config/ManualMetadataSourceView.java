package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Read view for manually-described metadata (`type: MANUAL`). {@code valid_until} is an ISO-8601
 * date-time; {@code signing_certificate} is the base64 certificate string, or null when none.
 */
public class ManualMetadataSourceView extends MetadataSourceView {

    @JsonProperty("entity_id")
    private final String entityId;

    @JsonProperty("valid_until")
    private final String validUntil;

    @JsonProperty("assertion_consumer_service")
    private final AssertionConsumerServiceView assertionConsumerService;

    @JsonProperty("signing_certificate")
    private final String signingCertificate;

    public ManualMetadataSourceView(String entityId, String validUntil,
        AssertionConsumerServiceView assertionConsumerService, String signingCertificate) {

        this.entityId = entityId;
        this.validUntil = validUntil;
        this.assertionConsumerService = assertionConsumerService;
        this.signingCertificate = signingCertificate;
    }

    public String getEntityId() {

        return entityId;
    }

    public String getValidUntil() {

        return validUntil;
    }

    public AssertionConsumerServiceView getAssertionConsumerService() {

        return assertionConsumerService;
    }

    public String getSigningCertificate() {

        return signingCertificate;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ManualMetadataSourceView that = (ManualMetadataSourceView) o;
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

        return "ManualMetadataSourceView{entityId='" + entityId + "', validUntil='" + validUntil
            + "', assertionConsumerService=" + assertionConsumerService
            + ", signingCertificate=" + (signingCertificate == null ? "null" : "<redacted>") + '}';
    }
}
