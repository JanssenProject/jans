package io.jans.shibboleth.trust.api.config.rs;

import java.util.List;
import java.util.UUID;

import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustStatus;
import io.jans.shibboleth.trust.config.profile.common.ProfileType;
import io.jans.shibboleth.trust.dto.config.CreateTrustRelationshipRequest;
import io.jans.shibboleth.trust.dto.config.MetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.Saml2ArtifactResolutionProfileConfigurationRequest;
import io.jans.shibboleth.trust.dto.config.Saml2AttributeQueryProfileConfigurationRequest;
import io.jans.shibboleth.trust.dto.config.Saml2EcpProfileConfigurationRequest;
import io.jans.shibboleth.trust.dto.config.Saml2LogoutProfileConfigurationRequest;
import io.jans.shibboleth.trust.dto.config.Saml2SsoProfileConfigurationRequest;
import io.jans.shibboleth.trust.dto.config.ShibbolethSsoProfileConfigurationRequest;
import io.jans.shibboleth.trust.dto.config.UpdateBasicInfoRequest;
import io.jans.shibboleth.trust.dto.config.UpdateReleasedAttributesRequest;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipRepository;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Response;

public class TrustRelationshipsConfigurationResource implements TrustRelationshipsApi {

    @Inject
    private TrustRelationshipRepository trustRelationshipRepository;

    @Override
    public Response activateTrustRelationship(UUID id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'activateTrustRelationship'");
    }

    @Override
    public Response cancelTrustRelationshipActivation(UUID id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'cancelTrustRelationshipActivation'");
    }

    @Override
    public Response createTrustRelationship(
            @Valid @NotNull CreateTrustRelationshipRequest createTrustRelationshipRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createTrustRelationship'");
    }

    @Override
    public Response deactivateTrustRelationship(UUID id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deactivateTrustRelationship'");
    }

    @Override
    public Response deleteTrustRelationship(UUID id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteTrustRelationship'");
    }

    @Override
    public Response getTrustRelationship(UUID id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTrustRelationship'");
    }

    @Override
    public Response getTrustRelationshipMetadataSource(UUID id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTrustRelationshipMetadataSource'");
    }

    @Override
    public Response getTrustRelationshipProfiles(UUID id, List<ProfileType> profiles) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTrustRelationshipProfiles'");
    }

    @Override
    public Response getTrustRelationshipReleasedAttributes(UUID id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTrustRelationshipReleasedAttributes'");
    }

    @Override
    public Response listTrustRelationships(TrustNature nature, TrustStatus status, String displayName,
            String description, @Min(1) Integer page, @Min(1) @Max(100) Integer size) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listTrustRelationships'");
    }

    @Override
    public Response setTrustRelationshipMetadataSource(UUID id,
            @Valid @NotNull MetadataSourceRequest metadataSourceRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTrustRelationshipMetadataSource'");
    }

    @Override
    public Response setTrustRelationshipReleasedAttributes(UUID id,
            @Valid @NotNull UpdateReleasedAttributesRequest updateReleasedAttributesRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTrustRelationshipReleasedAttributes'");
    }

    @Override
    public Response updateSaml2ArtifactResolutionProfileConfiguration(UUID id,
            @Valid @NotNull Saml2ArtifactResolutionProfileConfigurationRequest saml2ArtifactResolutionProfileConfigurationRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateSaml2ArtifactResolutionProfileConfiguration'");
    }

    @Override
    public Response updateSaml2AttributeQueryProfileConfiguration(UUID id,
            @Valid @NotNull Saml2AttributeQueryProfileConfigurationRequest saml2AttributeQueryProfileConfigurationRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateSaml2AttributeQueryProfileConfiguration'");
    }

    @Override
    public Response updateSaml2EcpProfileConfiguration(UUID id,
            @Valid @NotNull Saml2EcpProfileConfigurationRequest saml2EcpProfileConfigurationRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateSaml2EcpProfileConfiguration'");
    }

    @Override
    public Response updateSaml2LogoutProfileConfiguration(UUID id,
            @Valid @NotNull Saml2LogoutProfileConfigurationRequest saml2LogoutProfileConfigurationRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateSaml2LogoutProfileConfiguration'");
    }

    @Override
    public Response updateSaml2SsoProfileConfiguration(UUID id,
            @Valid @NotNull Saml2SsoProfileConfigurationRequest saml2SsoProfileConfigurationRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateSaml2SsoProfileConfiguration'");
    }

    @Override
    public Response updateShibbolethSsoProfileConfiguration(UUID id,
            @Valid @NotNull ShibbolethSsoProfileConfigurationRequest shibbolethSsoProfileConfigurationRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateShibbolethSsoProfileConfiguration'");
    }

    @Override
    public Response updateTrustRelationshipBasicInfo(UUID id,
            @Valid @NotNull UpdateBasicInfoRequest updateBasicInfoRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateTrustRelationshipBasicInfo'");
    }
    
}
