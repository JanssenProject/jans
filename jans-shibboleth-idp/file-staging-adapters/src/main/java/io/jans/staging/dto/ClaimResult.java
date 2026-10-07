package io.jans.staging.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Response body for a successful claim ({@code POST /v1/files/{token}/claim}): the durable path handle the
 * caller stores and reads directly, plus integrity metadata. {@code content_type} is omitted when none was
 * asserted at upload.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClaimResult {

    @JsonProperty("handle")
    private final String handle;

    @JsonProperty("size")
    private final long size;

    @JsonProperty("content_type")
    private final String contentType;

    @JsonProperty("sha256")
    private final String sha256;

    public ClaimResult(String handle, long size, String contentType, String sha256) {

        this.handle = handle;
        this.size = size;
        this.contentType = contentType;
        this.sha256 = sha256;
    }

    public String getHandle() {

        return handle;
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

    @Override
    public boolean equals(Object o) {

        if (this == o) {

            return true;
        }
        if (o == null || getClass() != o.getClass()) {

            return false;
        }
        ClaimResult that = (ClaimResult) o;
        return size == that.size
            && Objects.equals(handle, that.handle)
            && Objects.equals(contentType, that.contentType)
            && Objects.equals(sha256, that.sha256);
    }

    @Override
    public int hashCode() {

        return Objects.hash(handle, size, contentType, sha256);
    }

    @Override
    public String toString() {

        return "ClaimResult{handle='" + handle + "', size=" + size + ", contentType='" + contentType
            + "', sha256='" + sha256 + "'}";
    }
}
