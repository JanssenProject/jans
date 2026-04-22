package io.jans.shibboleth.model.metadata;

import java.util.Objects;

public class NoMetadataSource implements MetadataSource {

    private static final NoMetadataSource INSTANCE = new NoMetadataSource();

    public static final NoMetadataSource getInstance() {

        return INSTANCE;
    }

    @Override
    public MetadataSourceType getType() {

        return MetadataSourceType.NONE;
    }

    @Override
    public boolean equals(Object o) {

        if ( this == null ) return true;
        if ( o == null || NoMetadataSource.class != o.getClass() ) return false;

        return true;
    }

    @Override
    public int hashCode() {

        return Objects.hash(MetadataSourceType.NONE.toString());
    }

    @Override
    public String toString() {

        return MetadataSourceType.NONE.toString();
    }
}