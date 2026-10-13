package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.config.profile.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.config.profile.common.RequestSignatureValidationPolicy;

import java.util.List;
import java.util.Objects;

/**
 * Partial update of a trust relationship's SAML2 Artifact Resolution profile configuration. Every
 * field is optional; only the fields present in the request are changed, the rest keep their current
 * values. A dumb data holder — unknown properties are rejected.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class Saml2ArtifactResolutionProfileConfigurationRequest {

    @JsonProperty("status")
    private ProfileStatus status;

    @JsonProperty("inbound_flows")
    private List<String> inboundFlows;

    @JsonProperty("outbound_flows")
    private List<String> outboundFlows;

    @JsonProperty("message_signing_policy")
    private MessageSigningPolicy messageSigningPolicy;

    @JsonProperty("request_signature_validation_policy")
    private RequestSignatureValidationPolicy requestSignatureValidationPolicy;

    @JsonProperty("encryption_fallback_policy")
    private EncryptionFallbackPolicy encryptionFallbackPolicy;

    @JsonProperty("nameid_encryption_policy")
    private NameIdEncryptionPolicy nameIdEncryptionPolicy;

    @JsonProperty("assertion_signing_policy")
    private AssertionSigningPolicy assertionSigningPolicy;

    @JsonProperty("assertion_encryption_policy")
    private AssertionEncryptionPolicy assertionEncryptionPolicy;

    @JsonProperty("attribute_encryption_policy")
    private AttributeEncryptionPolicy attributeEncryptionPolicy;

    public Saml2ArtifactResolutionProfileConfigurationRequest() {
    }

    public ProfileStatus getStatus() {

        return status;
    }

    public void setStatus(ProfileStatus status) {

        this.status = status;
    }

    public List<String> getInboundFlows() {

        return inboundFlows;
    }

    public void setInboundFlows(List<String> inboundFlows) {

        this.inboundFlows = inboundFlows;
    }

    public List<String> getOutboundFlows() {

        return outboundFlows;
    }

    public void setOutboundFlows(List<String> outboundFlows) {

        this.outboundFlows = outboundFlows;
    }

    public MessageSigningPolicy getMessageSigningPolicy() {

        return messageSigningPolicy;
    }

    public void setMessageSigningPolicy(MessageSigningPolicy messageSigningPolicy) {

        this.messageSigningPolicy = messageSigningPolicy;
    }

    public RequestSignatureValidationPolicy getRequestSignatureValidationPolicy() {

        return requestSignatureValidationPolicy;
    }

    public void setRequestSignatureValidationPolicy(RequestSignatureValidationPolicy requestSignatureValidationPolicy) {

        this.requestSignatureValidationPolicy = requestSignatureValidationPolicy;
    }

    public EncryptionFallbackPolicy getEncryptionFallbackPolicy() {

        return encryptionFallbackPolicy;
    }

    public void setEncryptionFallbackPolicy(EncryptionFallbackPolicy encryptionFallbackPolicy) {

        this.encryptionFallbackPolicy = encryptionFallbackPolicy;
    }

    public NameIdEncryptionPolicy getNameIdEncryptionPolicy() {

        return nameIdEncryptionPolicy;
    }

    public void setNameIdEncryptionPolicy(NameIdEncryptionPolicy nameIdEncryptionPolicy) {

        this.nameIdEncryptionPolicy = nameIdEncryptionPolicy;
    }

    public AssertionSigningPolicy getAssertionSigningPolicy() {

        return assertionSigningPolicy;
    }

    public void setAssertionSigningPolicy(AssertionSigningPolicy assertionSigningPolicy) {

        this.assertionSigningPolicy = assertionSigningPolicy;
    }

    public AssertionEncryptionPolicy getAssertionEncryptionPolicy() {

        return assertionEncryptionPolicy;
    }

    public void setAssertionEncryptionPolicy(AssertionEncryptionPolicy assertionEncryptionPolicy) {

        this.assertionEncryptionPolicy = assertionEncryptionPolicy;
    }

    public AttributeEncryptionPolicy getAttributeEncryptionPolicy() {

        return attributeEncryptionPolicy;
    }

    public void setAttributeEncryptionPolicy(AttributeEncryptionPolicy attributeEncryptionPolicy) {

        this.attributeEncryptionPolicy = attributeEncryptionPolicy;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Saml2ArtifactResolutionProfileConfigurationRequest that = (Saml2ArtifactResolutionProfileConfigurationRequest) o;
        return status == that.status
            && Objects.equals(inboundFlows, that.inboundFlows)
            && Objects.equals(outboundFlows, that.outboundFlows)
            && messageSigningPolicy == that.messageSigningPolicy
            && requestSignatureValidationPolicy == that.requestSignatureValidationPolicy
            && encryptionFallbackPolicy == that.encryptionFallbackPolicy
            && nameIdEncryptionPolicy == that.nameIdEncryptionPolicy
            && assertionSigningPolicy == that.assertionSigningPolicy
            && assertionEncryptionPolicy == that.assertionEncryptionPolicy
            && attributeEncryptionPolicy == that.attributeEncryptionPolicy;
    }

    @Override
    public int hashCode() {

        return Objects.hash(status, inboundFlows, outboundFlows, messageSigningPolicy,
            requestSignatureValidationPolicy, encryptionFallbackPolicy, nameIdEncryptionPolicy,
            assertionSigningPolicy, assertionEncryptionPolicy, attributeEncryptionPolicy);
    }

    @Override
    public String toString() {

        return "Saml2ArtifactResolutionProfileConfigurationRequest{status=" + status
            + ", inboundFlows=" + inboundFlows + ", outboundFlows=" + outboundFlows
            + ", messageSigningPolicy=" + messageSigningPolicy
            + ", requestSignatureValidationPolicy=" + requestSignatureValidationPolicy
            + ", encryptionFallbackPolicy=" + encryptionFallbackPolicy
            + ", nameIdEncryptionPolicy=" + nameIdEncryptionPolicy
            + ", assertionSigningPolicy=" + assertionSigningPolicy
            + ", assertionEncryptionPolicy=" + assertionEncryptionPolicy
            + ", attributeEncryptionPolicy=" + attributeEncryptionPolicy + '}';
    }
}
