package io.jans.shibboleth.model.rules.state;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.core.TrustStatus;
import io.jans.shibboleth.model.core.diagnostics.ActivationStatus;
import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.error.TrustTransitionError;
import io.jans.shibboleth.model.metadata.MetadataSourceType;
import io.jans.shibboleth.model.util.TrustResult;

public final class TrustTransitionRules {
    
    private TrustTransitionRules() { }

    public static List<TrustTransitionRule> defaultRules() {

        return List.of(
            new TrustTransitionRule(
                TrustStatus.DRAFT,TrustStatus.READY,
                (ctx) -> ctx.hasRealMetadataSource() && ctx.hasAnyActiveProfileConfiguration(),
                "DRAFT --> READY: real metadatasource and any active profile configuration"
            ),

            new TrustTransitionRule(
                TrustStatus.READY,TrustStatus.DRAFT,
                (ctx) -> ctx.hasNoActiveProfileConfiguration(),
                "READY --> DRAFT: no active profile configuration"
            ),

            new TrustTransitionRule(
                TrustStatus.READY,TrustStatus.DRAFT,
                (ctx) -> ctx.hasNoRealMetadataSource(),
                "READY --> DRAFT: no real metadatasource"
            ),

            new TrustTransitionRule(
                TrustStatus.READY,TrustStatus.ACTIVATING,
                (ctx) -> ctx.activateCalled(),
                "READY --> ACTIVATING: activate() called"
            ),

            new TrustTransitionRule(
                TrustStatus.ACTIVATING,TrustStatus.READY,
                (ctx) -> ctx.cancelActivationCalled(), 
                "ACTIVATING --> READY: cancelActivation() called"
            ),

            new TrustTransitionRule(
                TrustStatus.ACTIVATING,TrustStatus.ACTIVE, 
                (ctx) -> ctx.finalizeActivationCalled() && ctx.hasSuccessfulActivationDiagnostics(),
                "ACTIVATING --> ACTIVE: finalizeActivation() called and successful activation diagnostics"
            ),

            new TrustTransitionRule(  
                TrustStatus.ACTIVATING,TrustStatus.READY, 
                (ctx) -> ctx.finalizeActivationCalled() && ctx.hasFailedActivationDiagnostics(),
                "ACTIVATING --> READY: finalizeActivation() called and failed activation diagnostics"
            ),

            new TrustTransitionRule(
                TrustStatus.ACTIVE,TrustStatus.INACTIVE,
                (ctx) -> ctx.deactivateCalled(),
                "ACTIVE --> INACTIVE: deactivate() called "
            ),

            new TrustTransitionRule(
                TrustStatus.INACTIVE,TrustStatus.ACTIVATING,
                (ctx) -> ctx.activateCalled() && ctx.hasRealMetadataSource() && ctx.hasAnyActiveProfileConfiguration(),
                "INACTIVE -> ACTIVE: activate() called and requirements met for ACTIVATING "
            ),

            new TrustTransitionRule(
                TrustStatus.INACTIVE,TrustStatus.DRAFT,
                (ctx) -> ctx.activateCalled() && (ctx.hasNoRealMetadataSource() || ctx.hasNoActiveProfileConfiguration()),
                "INACTIVE -> DRAFT: activate() called and no real metadatasource or no active profile configuration "
            ),

            new TrustTransitionRule( 
                TrustStatus.ACTIVE,TrustStatus.ACTIVATING,
                (ctx) -> ctx.updateMetadataSourceCalled() && ctx.hasRealMetadataSource() && ctx.hasMetadataSourceChanged(),
                "ACTIVE -> ACTIVATING: updateMetadataSource() called and real metadatasource <> previous metadatasource"
            ),

            new TrustTransitionRule(
                TrustStatus.ACTIVE,TrustStatus.ACTIVATING,
                (ctx) -> ctx.updateProfileConfigurationCalled() && ctx.hasAnyActiveProfileConfiguration() && ctx.hasAnyProfileConfigurationChanged(),
                "ACTIVE -> ACTIVATING : updateXXXProfileConfiguration() called and active profiles >=1 and profile configuration changed "
            ),

            new TrustTransitionRule( 
                TrustStatus.ACTIVE,TrustStatus.DRAFT,
                (ctx) -> ctx.updateProfileConfigurationCalled() && ctx.hasNoActiveProfileConfiguration(),
                "ACTIVE -> DRAFT : updateXXXProfileConfiguration() called and active profiles == 0"
            )
        );
    }

    public static List<TrustTransitionRule> withDefaultRules(List<TrustTransitionRule> existing) {

        if (existing == null) {

            return defaultRules();
        }
        
        return Stream.concat(defaultRules().stream(),existing.stream()).collect(Collectors.toList());
    }

    public static TrustResult<TrustStatus> determineNewStatus(BuildContext context) {

        return determineNewStatus(context,defaultRules());
    }

    public static TrustResult<TrustStatus> determineNewStatus(BuildContext context, List<TrustTransitionRule> rules) {
        
        if(context == null) {

            return TrustResult.failure(TrustTransitionError.contextRequired());
        }

        if (rules == null) {

            return TrustResult.failure(TrustTransitionError.rulesRequired());
        }

        TrustStatus nextstatus = rules.stream()
            .filter(rule -> rule.matches(context))
            .findFirst()
            .map(TrustTransitionRule::getTo)
            .orElse(context.getStatus());
    
        return TrustResult.success(nextstatus);
    }
}
