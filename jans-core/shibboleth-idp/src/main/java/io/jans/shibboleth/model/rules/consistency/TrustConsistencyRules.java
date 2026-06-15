package io.jans.shibboleth.model.rules.consistency;

import java.util.List;
import io.jans.shibboleth.model.BuildContext;
import io.jans.shibboleth.model.util.TrustResult;

public class TrustConsistencyRules {
    
    private TrustConsistencyRules() {}

    @FunctionalInterface
    public interface TrustConsistencyRule {

        TrustResult<Void> check(BuildContext context);
    }

    public static TrustResult<Void> enforce(BuildContext context) {


        return enforce(context,defaultRules());
    }

    public static TrustResult<Void> enforce(BuildContext context, List<TrustConsistencyRule> rules) {

        for (TrustConsistencyRule rule : rules) {
            TrustResult<Void> result = rule.check(context);
            if(result.isFailure()) {

                return result;
            }
        }

        return TrustResult.success(null);
    }

    private static final List<TrustConsistencyRule> defaultRules() {

        return List.of(
            RequiredFieldsRule::check,
            MetadataSourceCompatibilityRule::check
        );
    }
}
