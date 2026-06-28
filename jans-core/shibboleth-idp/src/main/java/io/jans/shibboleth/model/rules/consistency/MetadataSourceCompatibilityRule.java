package io.jans.shibboleth.model.rules.consistency;

import java.util.Objects;

import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.core.TrustNature;
import io.jans.shibboleth.model.error.OperationRestrictedToNature;
import io.jans.shibboleth.model.error.TrustError;
import io.jans.shibboleth.model.error.DomainObjectConsistencyFailed;
import io.jans.shibboleth.model.metadata.MetadataSource;
import io.jans.shibboleth.model.metadata.MetadataSourceType;

import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.util.TrustPredicates;
import io.jans.shibboleth.model.util.TrustResult;

public class MetadataSourceCompatibilityRule {
    
    private MetadataSourceCompatibilityRule() {}

    public static TrustResult<Void> check(BuildContext context) {

        if (context.getMetadataSource() == null || context.getNature() == null) {

            //skip rule application
            return TrustResult.success(null);
        }

        if ( !TrustPredicates.supportsMetadataSource(context, context.getMetadataSource())) {

            TrustNature required_nature = context.getNature() == TrustNature.INDIVIDUAL ? TrustNature.AGGREGATE : TrustNature.INDIVIDUAL;

            TrustError cause = OperationRestrictedToNature.of("updateMetadataSource",required_nature,context.getNature());
            return TrustResult.failure( DomainObjectConsistencyFailed.forClassWithCause(TrustRelationship.class, cause) );
        }

        return TrustResult.success(null);
    }
}
