package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Request body for setting a trust relationship's metadata source. Polymorphic on {@code type}: the
 * concrete shape is chosen by the discriminator.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = NoneMetadataSourceRequest.class, name = "NONE"),
    @JsonSubTypes.Type(value = FileMetadataSourceRequest.class, name = "FILE"),
    @JsonSubTypes.Type(value = UriMetadataSourceRequest.class, name = "URI"),
    @JsonSubTypes.Type(value = UpstreamMetadataSourceRequest.class, name = "UPSTREAM"),
    @JsonSubTypes.Type(value = MdqMetadataSourceRequest.class, name = "MDQ"),
    @JsonSubTypes.Type(value = ManualMetadataSourceRequest.class, name = "MANUAL")
})
public abstract class MetadataSourceRequest {
}
