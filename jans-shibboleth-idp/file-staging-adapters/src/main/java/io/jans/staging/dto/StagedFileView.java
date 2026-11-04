package io.jans.staging.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Response body for a successful upload ({@code POST /v1/files}): the opaque token plus integrity and
 * lifecycle metadata. {@code content_type} is omitted when the upload asserted none.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StagedFileView {

    @JsonProperty("token")
    private final String token;

    @JsonProperty("size")
    private final long size;

    @JsonProperty("content_type")
    private final String contentType;

    @JsonProperty("sha256")
    private final String sha256;

    @JsonProperty("expires_at")
    private final String expiresAt;

    public StagedFileView(String token, long size, String contentType, String sha256, String expiresAt) {

        this.token = token;
        this.size = size;
        this.contentType = contentType;
        this.sha256 = sha256;
        this.expiresAt = expiresAt;
    }

    public String getToken() {

        return token;
    }

    public long getSize() {

        return size;
    }

    public String getContentType() {

        return contentType;
    }

    public String getSha256() {

        return sha256;
    }

    public String getExpiresAt() {

        return expiresAt;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {

            return true;
        }
        if (o == null || getClass() != o.getClass()) {

            return false;
        }
        StagedFileView that = (StagedFileView) o;
        return size == that.size
            && Objects.equals(token, that.token)
            && Objects.equals(contentType, that.contentType)
            && Objects.equals(sha256, that.sha256)
            && Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {

        return Objects.hash(token, size, contentType, sha256, expiresAt);
    }

    @Override
    public String toString() {

        return "StagedFileView{token='" + token + "', size=" + size + ", contentType='" + contentType
            + "', sha256='" + sha256 + "', expiresAt='" + expiresAt + "'}";
    }
}
