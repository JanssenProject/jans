package io.jans.shibboleth.model.rules.operations;

import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.util.TrustResult;

import java.util.List;

public class TrustOperationRestrictions {

    private TrustOperationRestrictions() {}

    @FunctionalInterface
    public interface TrustOperationRestriction {

        TrustResult<Void> check(BuildContext context);
    }

    public static final TrustResult<Void> enforce(BuildContext context) {

        return enforce(context, defaultRestrictions());
    }

    public static final TrustResult<Void> enforce(BuildContext context, List<TrustOperationRestriction> restrictions) {

        for (TrustOperationRestriction restriction : restrictions) {

            TrustResult<Void> result = restriction.check(context);
            if (result.isFailure()) {

                return result;
            }
        }
        return TrustResult.success(null);
    }

    private static final List<TrustOperationRestriction> defaultRestrictions() {

        return List.of(
            StatusRestrictions.IncorporateDiscoveredEntityIdsRestriction::check,
            StatusRestrictions.UpdateMetadataSourceRestriction::check,
            StatusRestrictions.UpdateProfileConfigurationRestriction::check,
            StatusRestrictions.UpdateReleasedAttributesRestriction::check,
            StatusRestrictions.FinalizeActivationRestriction::check,
            StatusRestrictions.CancelActivationRestriction::check,
            StatusRestrictions.ActivateRestriction::check,
            StatusRestrictions.DeactivateRestriction::check,

            NatureRestrictions.IncorporateDiscoveredEntityIdsRestriction::check
        );
    }
}
