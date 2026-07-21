package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionTimeCondition;
import io.jans.shibboleth.trust.config.profile.common.AttributeStatementPolicy;
import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;

import java.util.List;
import java.util.Objects;

/**
 * Partial update of a trust relationship's Shibboleth SSO profile configuration. Every field is
 * optional; only the fields present in the request are changed, the rest keep their current values.
 * {@code max_authentication_age} and {@code assertion_lifetime} are ISO-8601 duration strings. A dumb
 * data holder — unknown properties are rejected.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ShibbolethSsoProfileConfigurationRequest {

    @JsonProperty("status")
    private ProfileStatus status;

    @JsonProperty("inbound_flows")
    private List<String> inboundFlows;

    @JsonProperty("outbound_flows")
    private List<String> outboundFlows;

    @JsonProperty("post_authentication_flows")
    private List<String> postAuthenticationFlows;

    @JsonProperty("authentication_result_reuse_policy")
    private AuthenticationResultReusePolicy authenticationResultReusePolicy;

    @JsonProperty("max_authentication_age")
    private String maxAuthenticationAge;

    @JsonProperty("message_signing_policy")
    private MessageSigningPolicy messageSigningPolicy;

    @JsonProperty("assertion_time_condition")
    private AssertionTimeCondition assertionTimeCondition;

    @JsonProperty("assertion_lifetime")
    private String assertionLifetime;

    @JsonProperty("assertion_signing_policy")
    private AssertionSigningPolicy assertionSigningPolicy;

    @JsonProperty("attribute_statement_policy")
    private AttributeStatementPolicy attributeStatementPolicy;

    @JsonProperty("nameid_format_precedence")
    private List<String> nameIdFormatPrecedence;

    public ShibbolethSsoProfileConfigurationRequest() {
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

    public List<String> getPostAuthenticationFlows() {

        return postAuthenticationFlows;
    }

    public void setPostAuthenticationFlows(List<String> postAuthenticationFlows) {

        this.postAuthenticationFlows = postAuthenticationFlows;
    }

    public AuthenticationResultReusePolicy getAuthenticationResultReusePolicy() {

        return authenticationResultReusePolicy;
    }

    public void setAuthenticationResultReusePolicy(AuthenticationResultReusePolicy authenticationResultReusePolicy) {

        this.authenticationResultReusePolicy = authenticationResultReusePolicy;
    }

    public String getMaxAuthenticationAge() {

        return maxAuthenticationAge;
    }

    public void setMaxAuthenticationAge(String maxAuthenticationAge) {

        this.maxAuthenticationAge = maxAuthenticationAge;
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

    public AttributeStatementPolicy getAttributeStatementPolicy() {

        return attributeStatementPolicy;
    }

    public void setAttributeStatementPolicy(AttributeStatementPolicy attributeStatementPolicy) {

        this.attributeStatementPolicy = attributeStatementPolicy;
    }

    public List<String> getNameIdFormatPrecedence() {

        return nameIdFormatPrecedence;
    }

    public void setNameIdFormatPrecedence(List<String> nameIdFormatPrecedence) {

        this.nameIdFormatPrecedence = nameIdFormatPrecedence;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShibbolethSsoProfileConfigurationRequest that = (ShibbolethSsoProfileConfigurationRequest) o;
        return status == that.status
            && Objects.equals(inboundFlows, that.inboundFlows)
            && Objects.equals(outboundFlows, that.outboundFlows)
            && Objects.equals(postAuthenticationFlows, that.postAuthenticationFlows)
            && authenticationResultReusePolicy == that.authenticationResultReusePolicy
            && Objects.equals(maxAuthenticationAge, that.maxAuthenticationAge)
            && messageSigningPolicy == that.messageSigningPolicy
            && assertionTimeCondition == that.assertionTimeCondition
            && Objects.equals(assertionLifetime, that.assertionLifetime)
            && assertionSigningPolicy == that.assertionSigningPolicy
            && attributeStatementPolicy == that.attributeStatementPolicy
            && Objects.equals(nameIdFormatPrecedence, that.nameIdFormatPrecedence);
    }

    @Override
    public int hashCode() {

        return Objects.hash(status, inboundFlows, outboundFlows, postAuthenticationFlows,
            authenticationResultReusePolicy, maxAuthenticationAge, messageSigningPolicy,
            assertionTimeCondition, assertionLifetime, assertionSigningPolicy, attributeStatementPolicy,
            nameIdFormatPrecedence);
    }

    @Override
    public String toString() {

        return "ShibbolethSsoProfileConfigurationRequest{status=" + status
            + ", inboundFlows=" + inboundFlows + ", outboundFlows=" + outboundFlows
            + ", postAuthenticationFlows=" + postAuthenticationFlows
            + ", authenticationResultReusePolicy=" + authenticationResultReusePolicy
            + ", maxAuthenticationAge='" + maxAuthenticationAge + '\''
            + ", messageSigningPolicy=" + messageSigningPolicy
            + ", assertionTimeCondition=" + assertionTimeCondition
            + ", assertionLifetime='" + assertionLifetime + '\''
            + ", assertionSigningPolicy=" + assertionSigningPolicy
            + ", attributeStatementPolicy=" + attributeStatementPolicy
            + ", nameIdFormatPrecedence=" + nameIdFormatPrecedence + '}';
    }
}
