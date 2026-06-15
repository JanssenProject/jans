package io.jans.shibboleth.model.rules.state;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.jans.shibboleth.model.BuildContext;
import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.core.TrustStatus;

public final class TrustTransitionRules {
    
    private TrustTransitionRules() { }

    public static List<TrustTransitionRule> defaultRules() {

        return List.of(

            new TrustTransitionRule(
                TrustStatus.DRAFT,TrustStatus.READY,
                (candidate) ->  hasRealMetadataSource(candidate) && hasAtLeastOneActiveProfileConfiguration(candidate),
                "DRAFT -> READY : Real metadatasource and at least one active profile"
            )
        );
    }

    public static List<TrustTransitionRule> withDefaultRules(List<TrustTransitionRule> existing) {

        if (existing == null) {

            return defaultRules();
        }
        
        return Stream.concat(defaultRules().stream(),existing.stream()).collect(Collectors.toList());
    }

    public static TrustStatus determineNewStatus(TrustRelationship candidate) {

        return determineNewStatus(candidate,defaultRules());
    }

    public static TrustStatus determineNewStatus(TrustRelationship candidate, List<TrustTransitionRule> rules) {
        
        if(candidate == null || rules == null) {

            return TrustStatus.DRAFT;
        }

        return rules.stream()
            .filter(rule -> rule.matches(candidate))
            .findFirst()
            .map(TrustTransitionRule::getTo)
            .orElse(candidate.getStatus());
    }

    private static boolean hasRealMetadataSource(TrustRelationship candidate) {

        return !candidate.hasNoMetadataSource();
    }

    private static boolean hasAtLeastOneActiveProfileConfiguration(TrustRelationship candidate) {

        return !candidate.hasNoActiveProfileConfiguration();
    }
}
