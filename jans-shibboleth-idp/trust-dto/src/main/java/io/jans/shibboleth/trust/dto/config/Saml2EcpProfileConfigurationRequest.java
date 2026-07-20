package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.config.profile.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionTimeCondition;
import io.jans.shibboleth.trust.config.profile.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeStatementPolicy;
import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.trust.config.profile.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.trust.config.profile.common.EndpointValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.config.profile.common.RequestSignatureValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.RequestSigningRequirement;

import java.util.List;
import java.util.Objects;

/**
 * Partial update of a trust relationship's SAML2 ECP profile configuration. Every field is optional;
 * only the fields present in the request are changed, the rest keep their current values.
 * {@code assertion_lifetime} and {@code maximum_sp_session_lifetime} are ISO-8601 duration strings.
 * A dumb data holder — unknown properties are rejected.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class Saml2EcpProfileConfigurationRequest {

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

    @JsonProperty("authentication_result_reuse_policy")
    private AuthenticationResultReusePolicy authenticationResultReusePolicy;

    @JsonProperty("assertion_encryption_policy")
    private AssertionEncryptionPolicy assertionEncryptionPolicy;

    @JsonProperty("attribute_encryption_policy")
    private AttributeEncryptionPolicy attributeEncryptionPolicy;

    @JsonProperty("maximum_sp_session_lifetime")
    private String maximumSpSessionLifetime;

    @JsonProperty("endpoint_validation_policy")
    private EndpointValidationPolicy endpointValidationPolicy;

    @JsonProperty("attribute_statement_policy")
    private AttributeStatementPolicy attributeStatementPolicy;

    @JsonProperty("friendly_name_randomization_policy")
    private FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy;

    @JsonProperty("nameid_format_precedence")
    private List<String> nameIdFormatPrecedence;

    @JsonProperty("request_signing_requirement")
    private RequestSigningRequirement requestSigningRequirement;

    public Saml2EcpProfileConfigurationRequest() {
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

    public AuthenticationResultReusePolicy getAuthenticationResultReusePolicy() {

        return authenticationResultReusePolicy;
    }

    public void setAuthenticationResultReusePolicy(AuthenticationResultReusePolicy authenticationResultReusePolicy) {

        this.authenticationResultReusePolicy = authenticationResultReusePolicy;
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

    public String getMaximumSpSessionLifetime() {

        return maximumSpSessionLifetime;
    }

    public void setMaximumSpSessionLifetime(String maximumSpSessionLifetime) {

        this.maximumSpSessionLifetime = maximumSpSessionLifetime;
    }

    public EndpointValidationPolicy getEndpointValidationPolicy() {

        return endpointValidationPolicy;
    }

    public void setEndpointValidationPolicy(EndpointValidationPolicy endpointValidationPolicy) {

        this.endpointValidationPolicy = endpointValidationPolicy;
    }

    public AttributeStatementPolicy getAttributeStatementPolicy() {

        return attributeStatementPolicy;
    }

    public void setAttributeStatementPolicy(AttributeStatementPolicy attributeStatementPolicy) {

        this.attributeStatementPolicy = attributeStatementPolicy;
    }

    public FriendlyNameRandomizationPolicy getFriendlyNameRandomizationPolicy() {

        return friendlyNameRandomizationPolicy;
    }

    public void setFriendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy) {

        this.friendlyNameRandomizationPolicy = friendlyNameRandomizationPolicy;
    }

    public List<String> getNameIdFormatPrecedence() {

        return nameIdFormatPrecedence;
    }

    public void setNameIdFormatPrecedence(List<String> nameIdFormatPrecedence) {

        this.nameIdFormatPrecedence = nameIdFormatPrecedence;
    }

    public RequestSigningRequirement getRequestSigningRequirement() {

        return requestSigningRequirement;
    }

    public void setRequestSigningRequirement(RequestSigningRequirement requestSigningRequirement) {

        this.requestSigningRequirement = requestSigningRequirement;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Saml2EcpProfileConfigurationRequest that = (Saml2EcpProfileConfigurationRequest) o;
        return status == that.status
            && Objects.equals(inboundFlows, that.inboundFlows)
            && Objects.equals(outboundFlows, that.outboundFlows)
            && messageSigningPolicy == that.messageSigningPolicy
            && assertionTimeCondition == that.assertionTimeCondition
            && Objects.equals(assertionLifetime, that.assertionLifetime)
            && assertionSigningPolicy == that.assertionSigningPolicy
            && requestSignatureValidationPolicy == that.requestSignatureValidationPolicy
            && encryptionFallbackPolicy == that.encryptionFallbackPolicy
            && nameIdEncryptionPolicy == that.nameIdEncryptionPolicy
            && authenticationResultReusePolicy == that.authenticationResultReusePolicy
            && assertionEncryptionPolicy == that.assertionEncryptionPolicy
            && attributeEncryptionPolicy == that.attributeEncryptionPolicy
            && Objects.equals(maximumSpSessionLifetime, that.maximumSpSessionLifetime)
            && endpointValidationPolicy == that.endpointValidationPolicy
            && attributeStatementPolicy == that.attributeStatementPolicy
            && friendlyNameRandomizationPolicy == that.friendlyNameRandomizationPolicy
            && Objects.equals(nameIdFormatPrecedence, that.nameIdFormatPrecedence)
            && requestSigningRequirement == that.requestSigningRequirement;
    }

    @Override
    public int hashCode() {

        return Objects.hash(status, inboundFlows, outboundFlows, messageSigningPolicy,
            assertionTimeCondition, assertionLifetime, assertionSigningPolicy,
            requestSignatureValidationPolicy, encryptionFallbackPolicy, nameIdEncryptionPolicy,
            authenticationResultReusePolicy, assertionEncryptionPolicy, attributeEncryptionPolicy,
            maximumSpSessionLifetime, endpointValidationPolicy, attributeStatementPolicy,
            friendlyNameRandomizationPolicy, nameIdFormatPrecedence, requestSigningRequirement);
    }

    @Override
    public String toString() {

        return "Saml2EcpProfileConfigurationRequest{status=" + status
            + ", assertionLifetime='" + assertionLifetime + '\''
            + ", maximumSpSessionLifetime='" + maximumSpSessionLifetime + '\''
            + ", endpointValidationPolicy=" + endpointValidationPolicy
            + ", requestSigningRequirement=" + requestSigningRequirement + ", ...}";
    }
}
