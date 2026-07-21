package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Read view of a trust relationship's metadata source. Polymorphic on {@code type}; the concrete
 * shape depends on the configured source. This is the response counterpart of
 * {@link MetadataSourceRequest} — note {@code FILE} exposes the stored {@code file_path} on read
 * (whereas the write accepts an upload {@code token}).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = NoneMetadataSourceView.class, name = "NONE"),
    @JsonSubTypes.Type(value = FileMetadataSourceView.class, name = "FILE"),
    @JsonSubTypes.Type(value = UriMetadataSourceView.class, name = "URI"),
    @JsonSubTypes.Type(value = UpstreamMetadataSourceView.class, name = "UPSTREAM"),
    @JsonSubTypes.Type(value = MdqMetadataSourceView.class, name = "MDQ"),
    @JsonSubTypes.Type(value = ManualMetadataSourceView.class, name = "MANUAL")
})
public abstract class MetadataSourceView {
}
