package io.jans.configapi.plugin.shibboleth.model.profile;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.Arrays;

@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "jansSAMLProfile")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Profile {

    @AttributeName
    @Schema(description = "Type of SAML Profile Type")
    private ProfileType profileType;

    @AttributeName
    @Schema(description = "Indicates if the profile is active or inactive")
    private ProfileStatus profileStatus;

    @AttributeName
    @Schema(description = "Array of names of inbound interceptor flows to run prior to message processing.")
    private String[] inboundFlows;

    @AttributeName
    @Schema(description = "Array of names of outbound interceptor flows to run prior to message processing.")
    private String[] outboundFlows;

    @AttributeName
    @Schema(description = "Array of names of inbound interceptor flows to run after successful authentication.")
    private String[] postAuthenticationFlows;

    @AttributeName
    @Schema(description = "Indicates if authentication results can be reused")
    private boolean authResultReuse;

    @AttributeName
    @Schema(description = "ISO-8601 duration format PnDTnHnMn.nS. E.g. PT15M. Indicates how long until an AuthenticationResult can be reused for the current request")
    private Duration maxAuthenticationAge;

    @AttributeName
    @Schema(description = "Policy regarding signing SAML messages for integrity and authenticity")
    private MessageSigningPolicy messageSigningPolicy;

    @AttributeName
    @Schema(description = "Indicates if request signature is to be validated or skiped")
    private boolean validationRequestSignature;

    @AttributeName
    @Schema(description = "Indicates if to automatically disable encryption if the RP doesn't possess a suitable key")
    private EncryptionFallbackPolicy encryptionFallbackPolicy;

    @AttributeName
    @Schema(description = "Indicates whether to encrypt name IDs")
    private boolean encryptNameId;

    @AttributeName
    @Schema(description = "Indicates whether to sign assertions")
    private boolean signAssertion;

    @AttributeName
    @Schema(description = "Indicates whether to encrypt assertions")
    private boolean encryptAssertion;

    @AttributeName
    @Schema(description = "Indicates whether to encrypt attributes")
    private boolean encryptAttribute;

    public ProfileType getProfileType() {
        return profileType;
    }

    public void setProfileType(ProfileType profileType) {
        this.profileType = profileType;
    }

    public ProfileStatus getProfileStatus() {
        return profileStatus;
    }

    public void setProfileStatus(ProfileStatus profileStatus) {
        this.profileStatus = profileStatus;
    }

    public String[] getInboundFlows() {
        return inboundFlows;
    }

    public void setInboundFlows(String[] inboundFlows) {
        this.inboundFlows = inboundFlows;
    }

    public String[] getOutboundFlows() {
        return outboundFlows;
    }

    public void setOutboundFlows(String[] outboundFlows) {
        this.outboundFlows = outboundFlows;
    }

    public boolean isPostAuthenticationFlows() {
        return postAuthenticationFlows;
    }

    public void setPostAuthenticationFlows(boolean postAuthenticationFlows) {
        this.postAuthenticationFlows = postAuthenticationFlows;
    }

    public boolean isAuthResultReuse() {
        return authResultReuse;
    }

    public void setAuthResultReuse(boolean authResultReuse) {
        this.authResultReuse = authResultReuse;
    }

    public Duration getMaxAuthenticationAge() {
        return maxAuthenticationAge;
    }

    public void setMaxAuthenticationAge(Duration maxAuthenticationAge) {
        this.maxAuthenticationAge = maxAuthenticationAge;
    }

    public MessageSigningPolicy getMessageSigningPolicy() {
        return messageSigningPolicy;
    }

    public void setMessageSigningPolicy(MessageSigningPolicy messageSigningPolicy) {
        this.messageSigningPolicy = messageSigningPolicy;
    }

    public boolean isValidationRequestSignature() {
        return validationRequestSignature;
    }

    public void setValidationRequestSignature(boolean validationRequestSignature) {
        this.validationRequestSignature = validationRequestSignature;
    }

    public EncryptionFallbackPolicy getEncryptionFallbackPolicy() {
        return encryptionFallbackPolicy;
    }

    public void setEncryptionFallbackPolicy(EncryptionFallbackPolicy encryptionFallbackPolicy) {
        this.encryptionFallbackPolicy = encryptionFallbackPolicy;
    }

    public boolean isEncryptNameId() {
        return encryptNameId;
    }

    public void setEncryptNameId(boolean encryptNameId) {
        this.encryptNameId = encryptNameId;
    }

    public boolean isSignAssertion() {
        return signAssertion;
    }

    public void setSignAssertion(boolean signAssertion) {
        this.signAssertion = signAssertion;
    }

    public boolean isEncryptAssertion() {
        return encryptAssertion;
    }

    public void setEncryptAssertion(boolean encryptAssertion) {
        this.encryptAssertion = encryptAssertion;
    }

    public boolean isEncryptAttribute() {
        return encryptAttribute;
    }

    public void setEncryptAttribute(boolean encryptAttribute) {
        this.encryptAttribute = encryptAttribute;
    }

    @Override
    public String toString() {
        return "Profile [profileType=" + profileType + ", profileStatus=" + profileStatus + ", inboundFlows="
                + Arrays.toString(inboundFlows) + ", outboundFlows=" + Arrays.toString(outboundFlows)
                + ", postAuthenticationFlows=" + postAuthenticationFlows + ", authResultReuse=" + authResultReuse
                + ", maxAuthenticationAge=" + maxAuthenticationAge + ", messageSigningPolicy=" + messageSigningPolicy
                + ", validationRequestSignature=" + validationRequestSignature + ", encryptionFallbackPolicy="
                + encryptionFallbackPolicy + ", encryptNameId=" + encryptNameId + ", signAssertion=" + signAssertion
                + ", encryptAssertion=" + encryptAssertion + ", encryptAttribute=" + encryptAttribute + "]";
    }

}