package io.jans.shibboleth.model.rules.state;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.jans.shibboleth.model.BuildContext;
import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.core.TrustStatus;
import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.error.TrustTransitionError;
import io.jans.shibboleth.model.util.TrustResult;

public final class TrustTransitionRules {
    
    private TrustTransitionRules() { }

    public static List<TrustTransitionRule> defaultRules() {

        return List.of(

            new TrustTransitionRule(
                TrustStatus.DRAFT,TrustStatus.READY,
                (candidate) ->  hasRealMetadataSource(candidate) && hasAtLeastOneActiveProfileConfiguration(candidate),
                "DRAFT -> READY : Real metadatasource and at least one active profile configuration"
            ),

            new TrustTransitionRule(
                TrustStatus.READY,TrustStatus.DRAFT,
                (candidate) -> hasNoRealMetadataSource(candidate) || hasNoActiveProfileConfiguration(candidate),
                "READY -> DRAFT : No real metadatasource or no active profile configuration"
            )
        );
    }

    public static List<TrustTransitionRule> withDefaultRules(List<TrustTransitionRule> existing) {

        if (existing == null) {

            return defaultRules();
        }
        
        return Stream.concat(defaultRules().stream(),existing.stream()).collect(Collectors.toList());
    }

    public static TrustResult<TrustStatus> determineNewStatus(TrustRelationship candidate) {

        return determineNewStatus(candidate,defaultRules());
    }

    public static TrustResult<TrustStatus> determineNewStatus(TrustRelationship candidate, List<TrustTransitionRule> rules) {
        
        if(candidate == null) {

            return TrustResult.failure(TrustTransitionError.candidateRequired());
        }

        if (rules == null) {

            return TrustResult.failure(TrustTransitionError.rulesRequired());
        }

        TrustStatus nextstatus = rules.stream()
            .filter(rule -> rule.matches(candidate))
            .findFirst()
            .map(TrustTransitionRule::getTo)
            .orElse(candidate.getStatus());
    
        return TrustResult.success(nextstatus);
    }

    private static boolean hasRealMetadataSource(TrustRelationship candidate) {

        return !candidate.hasNoMetadataSource();
    }

    private static boolean hasNoRealMetadataSource(TrustRelationship candidate) {

        return candidate.hasNoMetadataSource();
    }

    private static boolean hasAtLeastOneActiveProfileConfiguration(TrustRelationship candidate) {

        return !candidate.hasNoActiveProfileConfiguration();
    }

    private static boolean hasNoActiveProfileConfiguration(TrustRelationship candidate) {

        return candidate.hasNoActiveProfileConfiguration();
    }
}
