package io.jans.shibboleth.model.metadata;

import java.net.URI;

import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.util.TrustResult;

public class UriMetadataSource implements MetadataSource {
    
    private final URI uri;

    private UriMetadataSource(final URI uri) {

        this.uri = uri;
    }

    @Override
    public MetadataSourceType getType() {

        return MetadataSourceType.URI;
    }

    public static TrustResult<MetadataSource> of(URI uri) {

        if (uri == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("uri"));
        }
        
        return TrustResult.success(new UriMetadataSource(uri));
    }
}
