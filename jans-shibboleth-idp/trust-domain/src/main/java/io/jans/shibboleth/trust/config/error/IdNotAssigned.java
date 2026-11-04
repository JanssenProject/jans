package io.jans.shibboleth.trust.config.error;


public class IdNotAssigned extends TrustError {


    private IdNotAssigned(String message) {

        super(message);
    }
    
    public static IdNotAssigned forUpstreamMetadataSource() {

        return new IdNotAssigned("Upstream MetadataSources require a valid parent Id");
    }

    public static IdNotAssigned accessingValueOfUnassignedId() {

        return new IdNotAssigned("Method getValue() called on Id with unassigned value");
    }
}