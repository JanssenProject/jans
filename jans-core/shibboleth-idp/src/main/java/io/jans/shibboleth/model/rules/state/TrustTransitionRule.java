package io.jans.shibboleth.model.rules.state;

import java.util.function.Predicate;

import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.core.TrustStatus;
import io.jans.shibboleth.model.util.TrustResult;

public class TrustTransitionRule {

    private final TrustStatus from;
    private final TrustStatus to;
    private final Predicate<BuildContext> condition;
    private final String description;
    
    public TrustTransitionRule(TrustStatus from, TrustStatus to,Predicate<BuildContext> condition, String description ) {

        this.from = from;
        this.to = to;
        this.condition = condition != null ? condition : ctx -> false ;
        this.description = description!= null ? description : "";
    }

    public boolean matches(BuildContext context) {

        if (from == null || to == null) {

            return false;
        }

        return context.getStatus() == from && condition.test(context);
    }

    public TrustStatus getTo() {

        return to;
    }

    public String getDescription() {

        return description;
    }

}
