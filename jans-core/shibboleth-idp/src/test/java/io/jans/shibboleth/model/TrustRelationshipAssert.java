package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.Description;
import io.jans.shibboleth.model.core.DisplayName;
import io.jans.shibboleth.model.core.Id;
import io.jans.shibboleth.model.core.Version;
import io.jans.shibboleth.model.core.diagnostics.ActivationStatus;
import io.jans.shibboleth.model.metadata.MetadataSourceType;

import io.jans.shibboleth.model.core.TrustNature;
import io.jans.shibboleth.model.core.TrustStatus;

import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.config.profiles.common.ProfileStatus;
import io.jans.shibboleth.model.config.profiles.common.ProfileType;
import io.jans.shibboleth.model.config.profiles.ProfileConfigurationAssert;

import java.util.Arrays;
import java.util.Objects;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Lists;



public class TrustRelationshipAssert extends AbstractAssert<TrustRelationshipAssert,TrustRelationship> {

    public TrustRelationshipAssert(TrustRelationship actual) {

        super(actual,TrustRelationshipAssert.class);
    }

    public static TrustRelationshipAssert assertThat(TrustRelationship actual) {

        return new TrustRelationshipAssert(actual);
    }

    public TrustRelationshipAssert isNew() {

        isNotNull();

        if (actual.getId().isAssigned()) {
            failWithMessage("TrustRelationship with assigned id <%s> is not new",actual.getId());
        }
        return this;
    }

    public TrustRelationshipAssert hasDisplayName(DisplayName comparedValue) {

        isNotNull();

        if ( Objects.equals(actual.getDisplayName(),comparedValue) ) {

            return this;
        }

        failWithMessage("TrustRelationship displayName is <%s>. Expected: <%s>",actual.getDisplayName(),comparedValue);
        return this;
    }

    public TrustRelationshipAssert hasDescription(Description comparedValue) {

        isNotNull();

        if( Objects.equals(actual.getDescription(), comparedValue) ) {

            return this;
        }
        
        failWithMessage("TrustRelationship description is <%s>. Expected: <%s>",actual.getDescription(),comparedValue);
        return this;
    }

    public TrustRelationshipAssert isOfNature(TrustNature nature) {

        isNotNull();

        if(actual.getNature() != nature) {

            failWithMessage("TrustRelationship nature is <%s>. Expected: <%s>",actual.getNature(),nature);
        }
        return this;
    }

    public TrustRelationshipAssert isOfIndividualNature() {

        return isOfNature(TrustNature.INDIVIDUAL);
    }

    public TrustRelationshipAssert isOfAggregateNature() {

        return isOfNature(TrustNature.AGGREGATE);
    }

    public TrustRelationshipAssert isInStatus(TrustStatus status) {

        isNotNull();

        if(actual.getStatus() != status ) {
            
            failWithMessage("TrustRelationship status is <%s>. Expected: <%s>",actual.getStatus(),status);
        }

        return this;
    }

    public TrustRelationshipAssert isInDraftStatus() {

        return isInStatus(TrustStatus.DRAFT);
    }

    public TrustRelationshipAssert isInReadyStatus() {

        return isInStatus(TrustStatus.READY);
    }

    public TrustRelationshipAssert isInActivatingStatus() {

        return isInStatus(TrustStatus.ACTIVATING);
    }

    public TrustRelationshipAssert isInActiveStatus() {

        return isInStatus(TrustStatus.ACTIVE);
    }

    public TrustRelationshipAssert isInInactiveStatus() {

        return isInStatus(TrustStatus.INACTIVE);
    }

    public TrustRelationshipAssert doesNotHaveStatus(TrustStatus status) {

        isNotNull();

        if (actual.getStatus() == status ) {
            
            failWithMessage("TrustRelationship was expected to not have status <%s>",status);
        }

        return this;
    }
    
    public TrustRelationshipAssert isVersion(Version version) {

        isNotNull();

        if(!Objects.equals(actual.getVersion(), version)) {

            failWithMessage("TrustRelationship version is <%s>. Expected: <%s>",actual.getVersion(),version);
        }

        return this;
    }

    public TrustRelationshipAssert hasNoRealMetadataSource() {

        isNotNull();

        if(actual.hasNoRealMetadataSource()) {

            return this;
        }

        failWithMessage("TrustRelationship has metadata source type <%s>. Expected: <%s>",actual.getMetadataSource().getType(),MetadataSourceType.NONE);
        return this;
    }

    public TrustRelationshipAssert hasRealMetadataSource() {

        isNotNull();
        if(actual.hasNoRealMetadataSource()) {

            failWithMessage("TrustRelationship has no real metadata source (non-NONE).");
        }
        
        return this;
    }

    public TrustRelationshipAssert hasNoDiscoveredEntityIds() {

        isNotNull();

        if(actual.hasAnyDiscoveredEntityIds()) {

            failWithMessage("TrustRelationship has discovered entityIDs. Expected: none");
        }
        return this;
    }

    public TrustRelationshipAssert hasAnyDiscoveredEntityIds() {

        isNotNull();

        if(actual.hasNoDiscoveredEntityIds()) {

            failWithMessage("TrustRelationship has no discovered entityIDs. Expected: at least one");
        }

        return this;
    }

    public TrustRelationshipAssert hasNoActiveProfileConfiguration() {

        isNotNull();
        if (!actual.hasNoActiveProfileConfiguration()) {
           
            failWithMessage("TrustRelationship has at least one active profile configuration. Expected: none");
        }

        return this;
    }

    public TrustRelationshipAssert hasAtLeastOneActiveProfileConfiguration() {

        isNotNull();
        if (actual.hasNoActiveProfileConfiguration()) {

            failWithMessage("TrustRelationship has no active profile configuration. Expected at least one");
        }
        return this;
    }

    public TrustRelationshipAssert hasNoReleasedAttributes() {

        isNotNull();

        if (!actual.hasNoReleasedAttributes()) {

            failWithMessage("TrustRelationship has at least one released attribute. Expected: none");
        }

        return this;
    }

    public TrustRelationshipAssert hasNoActivationDiagnostics() {

        isNotNull();
        if (!actual.hasNoActivationDiagnostics()) {

            failWithMessage("TrustRelationship has associated activation diagnostics data. Expected: No activation diagnostics data");
        }
        
        return this;
    }

    public ProfileConfigurationAssert withProfileConfiguration(ProfileType profileType) {

        ProfileConfigurationAssert ret = null;

        switch(profileType) {
            case SHIBBOLETH_SSO:
                ret = ProfileConfigurationAssert.assertThat(actual.getShibbolethSsoProfileConfiguration(),profileType);
                break;
            case SAML2_ATTRIBUTE_QUERY:
                ret = ProfileConfigurationAssert.assertThat(actual.getSaml2AttributeQueryProfileConfiguration(),profileType);
                break;
            case SAML2_ARTIFACT_RESOLUTION:
                ret = ProfileConfigurationAssert.assertThat(actual.getSaml2ArtifactResolutionProfileConfiguration(),profileType);
                break;
            case SAML2_ECP:
                ret = ProfileConfigurationAssert.assertThat(actual.getSaml2EcpProfileConfiguration(),profileType);
                break;
            case SAML2_LOGOUT:
                ret = ProfileConfigurationAssert.assertThat(actual.getSaml2LogoutProfileConfiguration(),profileType);
                break;
            case SAML2_SSO:
                ret = ProfileConfigurationAssert.assertThat(actual.getSaml2SsoProfileConfiguration(),profileType);
                break;
            default:
                failWithMessage("Test support for Trustrelationship Profile type <%s> is missing",profileType);
                break;
        }

        return ret;
    }
    
}