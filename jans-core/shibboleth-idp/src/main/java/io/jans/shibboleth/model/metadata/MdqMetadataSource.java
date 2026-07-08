package io.jans.shibboleth.model.metadata;

import java.net.URI;

import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.util.TrustResult;

public class MdqMetadataSource implements MetadataSource {

    private final URI baseUrl;

    private MdqMetadataSource(URI baseUrl) {

        this.baseUrl = baseUrl;
    }

    @Override
    public MetadataSourceType getType() {

        return MetadataSourceType.MDQ;
    }

    public static TrustResult<MdqMetadataSource> of(URI baseUrl) {

        if (baseUrl == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("baseUrl"));
        }

        return TrustResult.success(new MdqMetadataSource(baseUrl));
    }
}
