package io.jans.shibboleth.trust.config.metadata;

public sealed interface MetadataSource permits
    NoMetadataSource,
    FileMetadataSource,
    UriMetadataSource,
    UpstreamMetadataSource,
    MdqMetadataSource,
    ManualMetadataSource {

    public MetadataSourceType getType();
}