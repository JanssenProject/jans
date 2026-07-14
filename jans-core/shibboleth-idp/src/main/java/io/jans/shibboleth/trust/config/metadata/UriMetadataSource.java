package io.jans.shibboleth.trust.config.metadata;

import java.net.URI;
import java.util.Objects;

import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;
import io.jans.shibboleth.trust.shared.Result;

public class UriMetadataSource implements MetadataSource {
    
    private final URI uri;

    private UriMetadataSource(final URI uri) {

        this.uri = uri;
    }

    @Override
    public MetadataSourceType getType() {

        return MetadataSourceType.URI;
    }

    public URI getUri() {

        return uri;
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;

        if (o == null || getClass() != o.getClass()) return false;

        UriMetadataSource other = (UriMetadataSource) o;

        return Objects.equals(uri,other.uri);
    }

    @Override
    public int hashCode() {

        return Objects.hash(uri);
    }

    public static Result<MetadataSource> of(URI uri) {

        if (uri == null) {

            return Result.failure(CannotBeNullOrBlank.forField("uri"));
        }
        
        return Result.success(new UriMetadataSource(uri));
    }
}
