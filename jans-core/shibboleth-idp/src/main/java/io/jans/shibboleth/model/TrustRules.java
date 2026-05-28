package io.jans.shibboleth.model;

import java.util.List;

import io.jans.shibboleth.model.rules.MetadataSourceCompatibilityRule;
import io.jans.shibboleth.model.rules.RequiredFieldsRule;
import io.jans.shibboleth.model.util.TrustResult;

public class TrustRules {
    
    private TrustRules() {}

    @FunctionalInterface
    public interface TrustRule {

        TrustResult<Void> check(BuildContext context);
    }

    public static TrustResult<Void> enforce(BuildContext context) {


        return enforce(context,defaultRules());
    }

    public static TrustResult<Void> enforce(BuildContext context, List<TrustRule> rules) {

        for (TrustRule rule : rules) {
            TrustResult<Void> result = rule.check(context);
            if(result.isFailure()) {

                return result;
            }
        }

        return TrustResult.success(null);
    }

    private static final List<TrustRule> defaultRules() {

        return List.of(
            RequiredFieldsRule::check,
            MetadataSourceCompatibilityRule::check
        );
    }
}
