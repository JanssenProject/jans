package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Metadata loaded from an uploaded file (`type: FILE`). The file is uploaded out-of-band, which
 * yields a {@code token}; that token is what the client sends here. Resolving and verifying the
 * token against the stored file is handled elsewhere — from this layer's point of view it is an
 * opaque reference.
 */
public class FileMetadataSourceRequest extends MetadataSourceRequest {

    @JsonProperty("token")
    private String token;

    public FileMetadataSourceRequest() {
    }

    public FileMetadataSourceRequest(String token) {

        this.token = token;
    }

    public String getToken() {

        return token;
    }

    public void setToken(String token) {

        this.token = token;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return Objects.equals(token, ((FileMetadataSourceRequest) o).token);
    }

    @Override
    public int hashCode() {

        return Objects.hash(token);
    }

    @Override
    public String toString() {

        return "FileMetadataSourceRequest{token='" + token + "'}";
    }
}
