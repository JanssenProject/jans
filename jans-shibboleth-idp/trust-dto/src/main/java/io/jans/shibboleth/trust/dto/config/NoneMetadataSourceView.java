package io.jans.shibboleth.trust.dto.config;

/**
 * Read view for a trust relationship with no metadata source (`type: NONE`).
 */
public class NoneMetadataSourceView extends MetadataSourceView {

    public NoneMetadataSourceView() {
    }

    @Override
    public boolean equals(Object o) {

        return o instanceof NoneMetadataSourceView;
    }

    @Override
    public int hashCode() {

        return NoneMetadataSourceView.class.hashCode();
    }

    @Override
    public String toString() {

        return "NoneMetadataSourceView{}";
    }
}
