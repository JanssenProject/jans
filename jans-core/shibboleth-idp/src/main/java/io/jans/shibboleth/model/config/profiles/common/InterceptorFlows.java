package io.jans.shibboleth.model.config.profiles.common;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class InterceptorFlows {

    private final List<String> flows;
    

    private InterceptorFlows(List<String> flows) {

        this.flows  = flows != null ? List.copyOf(flows) : List.of() ;
    }


    public static InterceptorFlows empty() {

        return new InterceptorFlows(null);
    }

    public static InterceptorFlows of(List<String> flows) {

        List<String> newflows = flows.stream()
            .map(flow -> { return flow.trim(); })
            .filter( flow -> { return !flow.isBlank(); })
            .collect(Collectors.toList());
        
        return new InterceptorFlows(newflows);
    }

    public boolean hasNone() {

        return flows.isEmpty();
    }

    public boolean hasSome() {

        return !flows.isEmpty();
    }
}