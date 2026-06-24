package io.jans.shibboleth.model.rules.consistency;

import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.util.TrustResult;

public class RequiredFieldsRule {
    
    private RequiredFieldsRule() {}

    public static TrustResult<Void> check(BuildContext context) {

        if (context.getId() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("id"));
        }

        if (context.getDisplayName() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("displayName"));
        }

        if (context.getDescription() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("description"));
        }

        if (context.getNature() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("nature"));
        }

        if (context.getVersion() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("version"));
        }

        if (context.getStatus() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("status"));
        }

        if (context.getMetadataSource() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("metadataSource"));
        }

        if (context.getShibbolethSsoProfileConfiguration() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("shibbolethSsoProfileConfiguration"));
        }

        if (context.getSaml2ArtifactResolutionProfileConfiguration() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("saml2ArtifactResolutionProfileConfiguration"));
        }

        if (context.getSaml2AttributeQueryProfileConfiguration() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("saml2AttributeQueryProfileConfiguration"));
        }

        if (context.getSaml2EcpProfileConfiguration() == null) {
            
            return TrustResult.failure(CannotBeNullOrBlank.forField("saml2EcpProfileConfiguration"));
        }

        if (context.getSaml2SsoProfileConfiguration() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("saml2SsoProfileConfiguration"));
        }

        if (context.getSaml2LogoutProfileConfiguration() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("saml2LogoutProfileConfiguration"));
        }

        if(context.getReleasedAttributes() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("releasedAttributes"));
        }

        if (context.getActivationDiagnostics() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("activationDiagnostics"));
        }

        if (context.getDiscoveredEntityIds() == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("discoveredEntityIds"));
        }

        return TrustResult.success(null);
    }
}
