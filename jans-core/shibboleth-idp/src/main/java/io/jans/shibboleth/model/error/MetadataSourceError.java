package io.jans.shibboleth.model.error;

public class MetadataSourceError extends TrustError {
    
    private MetadataSourceError(String message) {

        super(message);
    }

    public static MetadataSourceError required() {

        return new MetadataSourceError("MetadataSouce is required");
    }
}
