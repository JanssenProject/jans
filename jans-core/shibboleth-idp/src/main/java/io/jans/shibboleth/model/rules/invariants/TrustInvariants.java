package io.jans.shibboleth.model.rules.invariants;

import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.util.TrustResult;

import java.util.List;

public class TrustInvariants {
    
    private TrustInvariants() {}

    @FunctionalInterface
    public interface TrustInvariant {

        TrustResult<Void> check(BuildContext context);
    }

    public static final TrustResult<Void> enforce(BuildContext context) {

        return enforce(context,defaultInvariants());
    }

    public static final TrustResult<Void> enforce(BuildContext context, List<TrustInvariant> invariants) {

        for(TrustInvariant invariant : invariants) {

            TrustResult<Void> result = invariant.check(context);
            if(result.isFailure()) {

                return result;
            }
        }
        return TrustResult.success(null);
    }

    private static final List<TrustInvariant> defaultInvariants() {

        return List.of(
            PresenceInvariants.IdRequired::check,
            PresenceInvariants.DisplayNameRequired::check,
            PresenceInvariants.DescriptionRequired::check,
            PresenceInvariants.NatureRequired::check,
            PresenceInvariants.VersionRequired::check,
            PresenceInvariants.StatusRequired::check,
            PresenceInvariants.MetadataSourceRequired::check,
            PresenceInvariants.ShibbolethSsoProfileConfigurationRequired::check,
            PresenceInvariants.Saml2ArtifactResolutionProfileConfigurationRequired::check,
            PresenceInvariants.Saml2AttributeQueryProfileConfigurationRequired::check,
            PresenceInvariants.Saml2EcpProfileConfigurationRequired::check,
            PresenceInvariants.Saml2SsoProfileConfigurationRequired::check,
            PresenceInvariants.Saml2LogoutProfileConfigurationRequired::check,
            PresenceInvariants.ReleasedAttributesRequired::check,
            PresenceInvariants.ActivationDiagnosticsRequired::check,
            PresenceInvariants.DiscoveredEntityIdsRequired::check,
            CompatibilityInvariants.MetadataSourceCompatibility::check
        );
    }

}
