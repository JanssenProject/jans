package io.jans.shibboleth.model.metadata;

import java.net.URI;
import java.util.Objects;

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

    public URI getBaseUrl() {

        return baseUrl;
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;

        if (o == null || getClass() != o.getClass()) return false;

        MdqMetadataSource other = (MdqMetadataSource) o;

        return Objects.equals(baseUrl,other.baseUrl);
    }

    @Override
    public int hashCode() {

        return Objects.hash(baseUrl);
    }

    public static TrustResult<MdqMetadataSource> of(URI baseUrl) {

        if (baseUrl == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("baseUrl"));
        }

        return TrustResult.success(new MdqMetadataSource(baseUrl));
    }
}
