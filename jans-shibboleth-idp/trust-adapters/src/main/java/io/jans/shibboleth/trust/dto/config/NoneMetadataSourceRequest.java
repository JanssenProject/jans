package io.jans.shibboleth.trust.dto.config;

/**
 * Clears the metadata source (`type: NONE`). Carries no fields.
 */
public class NoneMetadataSourceRequest extends MetadataSourceRequest {

    public NoneMetadataSourceRequest() {
    }

    @Override
    public boolean equals(Object o) {

        return o instanceof NoneMetadataSourceRequest;
    }

    @Override
    public int hashCode() {

        return NoneMetadataSourceRequest.class.hashCode();
    }

    @Override
    public String toString() {

        return "NoneMetadataSourceRequest{}";
    }
}
