package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.config.profile.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionTimeCondition;
import io.jans.shibboleth.trust.config.profile.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.trust.config.profile.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.config.profile.common.RequestSignatureValidationPolicy;

import java.util.List;

/**
 * Read view of a trust relationship's SAML2 Attribute Query profile configuration.
 */
public class Saml2AttributeQueryProfileConfigurationView {

    @JsonProperty("status")
    private ProfileStatus status;

    @JsonProperty("inbound_flows")
    private List<String> inboundFlows;

    @JsonProperty("outbound_flows")
    private List<String> outboundFlows;

    @JsonProperty("message_signing_policy")
    private MessageSigningPolicy messageSigningPolicy;

    @JsonProperty("assertion_time_condition")
    private AssertionTimeCondition assertionTimeCondition;

    @JsonProperty("assertion_lifetime")
    private String assertionLifetime;

    @JsonProperty("assertion_signing_policy")
    private AssertionSigningPolicy assertionSigningPolicy;

    @JsonProperty("request_signature_validation_policy")
    private RequestSignatureValidationPolicy requestSignatureValidationPolicy;

    @JsonProperty("encryption_fallback_policy")
    private EncryptionFallbackPolicy encryptionFallbackPolicy;

    @JsonProperty("nameid_encryption_policy")
    private NameIdEncryptionPolicy nameIdEncryptionPolicy;

    @JsonProperty("assertion_encryption_policy")
    private AssertionEncryptionPolicy assertionEncryptionPolicy;

    @JsonProperty("attribute_encryption_policy")
    private AttributeEncryptionPolicy attributeEncryptionPolicy;

    @JsonProperty("friendly_name_randomization_policy")
    private FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy;

    public Saml2AttributeQueryProfileConfigurationView() {
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

    public AssertionTimeCondition getAssertionTimeCondition() {

        return assertionTimeCondition;
    }

    public void setAssertionTimeCondition(AssertionTimeCondition assertionTimeCondition) {

        this.assertionTimeCondition = assertionTimeCondition;
    }

    public String getAssertionLifetime() {

        return assertionLifetime;
    }

    public void setAssertionLifetime(String assertionLifetime) {

        this.assertionLifetime = assertionLifetime;
    }

    public AssertionSigningPolicy getAssertionSigningPolicy() {

        return assertionSigningPolicy;
    }

    public void setAssertionSigningPolicy(AssertionSigningPolicy assertionSigningPolicy) {

        this.assertionSigningPolicy = assertionSigningPolicy;
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

    public FriendlyNameRandomizationPolicy getFriendlyNameRandomizationPolicy() {

        return friendlyNameRandomizationPolicy;
    }

    public void setFriendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy) {

        this.friendlyNameRandomizationPolicy = friendlyNameRandomizationPolicy;
    }

}
