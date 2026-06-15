package io.jans.shibboleth.model.rules.state;

import java.util.function.Predicate;

import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.core.TrustStatus;
import io.jans.shibboleth.model.util.TrustResult;

public class TrustTransitionRule {

    private final TrustStatus from;
    private final TrustStatus to;
    private final Predicate<TrustRelationship> condition;
    private final String description;
    
    public TrustTransitionRule(TrustStatus from, TrustStatus to,Predicate<TrustRelationship> condition, String description ) {

        this.from = from;
        this.to = to;
        this.condition = condition != null ? condition : ctx -> false ;
        this.description = description!= null ? description : "";
    }

    public boolean matches(TrustRelationship candidate) {

        if (candidate == null || from == null) {

            return false;
        }

        return candidate.getStatus() == from && condition.test(candidate);
    }

    public TrustStatus getTo() {

        return to;
    }

    public String getDescription() {

        return description;
    }

}
