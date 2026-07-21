package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionTimeCondition;
import io.jans.shibboleth.trust.config.profile.common.AttributeStatementPolicy;
import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;

import java.util.List;

/**
 * Read view of a trust relationship's Shibboleth SSO profile configuration.
 */
public class ShibbolethSsoProfileConfigurationView {

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

    public ShibbolethSsoProfileConfigurationView() {
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

}
