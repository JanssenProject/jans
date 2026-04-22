package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.Description;
import io.jans.shibboleth.model.core.DisplayName;
import io.jans.shibboleth.model.core.Id;

import io.jans.shibboleth.model.metadata.MetadataSourceType;

import io.jans.shibboleth.model.core.TrustNature;
import io.jans.shibboleth.model.core.TrustStatus;

import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.config.profiles.common.ProfileType;
import io.jans.shibboleth.model.config.profiles.ProfileConfigurationAssert;
import io.jans.shibboleth.model.config.profiles.ProfileConfigurationWrapper;

import java.util.Objects;

import org.assertj.core.api.AbstractAssert;



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

    public TrustRelationshipAssert hasDisplayName(String displayName) {

        isNotNull();

        DisplayName comparedValued = DisplayName.of(displayName).getValue();

        if ( Objects.equals(actual.getDisplayName(),comparedValued) ) {

            return this;
        }

        failWithMessage("TrustRelationship displayName is <%s>. Expected: <%s>",actual.getDisplayName(),displayName);
        return this;
    }

    public TrustRelationshipAssert hasDescription(String description) {

        isNotNull();

        if( Objects.equals(actual.getDescription(), Description.of(description)) ) {

            return this;
        }
        
        failWithMessage("TrustRelationship description is <%s>. Expected: <%s>",actual.getDescription(),description);
        return this;
    }

    public TrustRelationshipAssert isOfNature(TrustNature nature) {

        isNotNull();

        if(actual.getNature() != nature) {

            failWithMessage("TrustRelationship nature is <%s>. Expected: <%s>",actual.getNature(),nature);
        }
        return this;
    }

    public TrustRelationshipAssert hasStatus(TrustStatus status) {

        isNotNull();

        if(actual.getStatus() != status ) {
            
            failWithMessage("TrustRelationship status is <%s>. Expected: <%s>",actual.getStatus(),status);
        }

        return this;
    }
    
    public TrustRelationshipAssert isVersion(int version) {

        isNotNull();

        if(actual.getVersion() != version ) {

            failWithMessage("TrustRelationship version is <%d>. Expected: <%d>",actual.getVersion(),version);
        }

        return this;
    }

    public TrustRelationshipAssert hasNoMetadataSource() {

        isNotNull();

        if(actual.hasNoMetadataSource()) {

            return this;
        }

        failWithMessage("TrustRelationship has metadata source type <%s>. Expected: <%s>",actual.getMetadataSource().getType(),MetadataSourceType.NONE);
        return this;
    }

    public TrustRelationshipAssert hasNoDiscoveredEntityIds() {

        isNotNull();

        if(actual.hasAnyDiscoveredEntityIds()) {

            failWithMessage("TrustRelationship has discovered entityIDs");
        }
        return this;
    }

    public TrustRelationshipAssert hasNoRegisteredIdpInstances() {

        isNotNull();

        if(actual.hasAnyRegisteredIdpInstances()) {

            failWithMessage("TrustRelationship has at least one registered idp instance");
        }
        return this;
    }

    public TrustRelationshipAssert hasNoWorkItem() {

        isNotNull();
        if(actual.hasAnyWorkItem()) {

            failWithMessage("TrustRelationship has at least one work item");
        }
        return this;
    }

    public ProfileConfigurationAssert withProfileConfiguration(ProfileType profileType) {

        ProfileConfigurationAssert ret = null;

        switch(profileType) {
            case SHIBBOLETH_SSO:
                ret = ProfileConfigurationAssert.assertThat(new ProfileConfigurationWrapper(actual.getShibbolethSsoProfileConfiguration(),profileType));
                break;
            case SAML2_ATTRIBUTE_QUERY:
                ret = ProfileConfigurationAssert.assertThat(new ProfileConfigurationWrapper(actual.getSaml2AttributeQueryProfileConfiguration(),profileType));
                break;
            case SAML2_ARTIFACT_RESOLUTION:
                ret = ProfileConfigurationAssert.assertThat(new ProfileConfigurationWrapper(actual.getSaml2ArtifactResolutionProfileConfiguration(),profileType));
                break;
            case SAML2_ECP:
                ret = ProfileConfigurationAssert.assertThat(new ProfileConfigurationWrapper(actual.getSaml2EcpProfileConfiguration(),profileType));
                break;
            case SAML2_LOGOUT:
                ret = ProfileConfigurationAssert.assertThat(new ProfileConfigurationWrapper(actual.getSaml2LogoutProfileConfiguration(),profileType));
                break;
            case SAML2_SSO:
                ret = ProfileConfigurationAssert.assertThat(new ProfileConfigurationWrapper(actual.getSaml2SsoProfileConfiguration(),profileType));
                break;
            default:
                failWithMessage("Test support for Trustrelationship Profile type <%s> is missing",profileType);
                break;
        }

        return ret;
    }
    
}