package io.jans.shibboleth.model.profiles.config;

import java.util.List;

public final class InterceptorFlows {

    private List<String> inbound;
    private List<String> outbound;
    private List<String> postAuthn;

    private InterceptorFlows(List<String> inbound, List<String> outbound,List<String> postAuthn) {

        this.inbound = inbound != null ? List.copyOf(inbound) : List.of() ;
        this.outbound = outbound != null ? List.copyOf(outbound) : List.of() ;
        this.postAuthn = postAuthn != null ? List.copyOf(postAuthn) : List.of() ;
    }


    public static InterceptorFlows empty() {

        return new InterceptorFlows(null,null,null);
    }

    public boolean hasAnyFlows() {

        return !inbound.isEmpty() && !outbound.isEmpty() && !postAuthn.isEmpty();
    }

    public boolean hasNoFlows() {

        return ! hasAnyFlows();
    }
}