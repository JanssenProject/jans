package io.jans.shibboleth.trust.config.profile.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

        if (flows == null || flows.isEmpty() ) {
            return empty();
        }

        List<String> newflows = flows.stream()
            .map(flow -> { return flow.trim(); })
            .filter( flow -> { return !flow.isBlank(); })
            .collect(Collectors.toList());
        
        return new InterceptorFlows(newflows);
    }

    public static InterceptorFlows from(InterceptorFlows existing) {

        return new InterceptorFlows(existing.flows);
    }

    public boolean hasNone() {

        return flows.isEmpty();
    }

    public boolean hasSome() {

        return !flows.isEmpty();
    }

    public List<String> getFlows() {

        return flows;
    }

    public boolean contains(String flow) {

        if (flow == null) 
            return false;

        return flows.contains(flow.trim());
    }

    public boolean containsAny(Collection<String> candidates) {

        if (candidates == null || candidates.isEmpty()) {

            return false;
        }

        return candidates.stream()
            .map(String::trim)
            .anyMatch(flows::contains);
    }

    public InterceptorFlows add(String ... flowsToAdd) {

        if (flowsToAdd == null || flowsToAdd.length == 0) {

            return this;
        }

        Set<String> merged = new LinkedHashSet<>(this.flows);
        for (String f: flowsToAdd) {
            String cleaned = f == null ? null: f.trim();
            if ( cleaned != null && !cleaned.isBlank() ) {
                merged.add(cleaned);
            }
        }

        return new InterceptorFlows(new ArrayList<>(merged));
    }

    public InterceptorFlows addAll(Collection<String> flowsToAdd) {

        if (flowsToAdd == null || flowsToAdd.isEmpty() ) {

            return this;
        }
        return add(flowsToAdd.toArray(new String[0]));
    }

    public InterceptorFlows remove(String ... flowsToRemove) {

        if (flowsToRemove == null || flowsToRemove.length == 0 || flows.isEmpty() ) {

            return this;
        }

        Set<String> toRemove = Arrays.stream(flowsToRemove)
            .map(s -> s == null ? null: s.trim())
            .filter( s -> s != null && !s.isBlank())
            .collect(Collectors.toSet());
        
        if (toRemove.isEmpty()) {

            return this;
        }

        List<String> remaining = this.flows.stream()
            .filter(f -> !toRemove.contains(f))
            .collect(Collectors.toList());
        
        return new InterceptorFlows(remaining);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        InterceptorFlows that = (InterceptorFlows) o;
        return flows.equals(that.flows);
    }

    @Override
    public int hashCode() {

        return flows.hashCode();
    }
}