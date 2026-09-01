package io.jans.staging.dto.mapper;

import io.jans.kernel.Result;
import io.jans.staging.Destination;
import io.jans.staging.StagedFile;
import io.jans.staging.dto.ClaimRequest;
import io.jans.staging.dto.ClaimResult;
import io.jans.staging.dto.StagedFileView;

/**
 * Translates between the {@link StagedFile} aggregate and the file-staging DTOs. Absent content type maps
 * to a {@code null} JSON field (omitted by the DTOs); {@code expires_at} is an ISO-8601 instant string.
 */
public final class StagedFileMapper {

    private StagedFileMapper() {
    }

    /**
     * Projects a freshly-staged file onto the upload response view.
     */
    public static StagedFileView toView(StagedFile file) {

        return new StagedFileView(
            file.token().getValue(),
            file.size(),
            contentTypeOrNull(file),
            file.contentHash().getValue(),
            file.expiresAt().toString());
    }

    /**
     * Projects a claimed file onto the claim response, carrying the durable handle and integrity metadata.
     */
    public static ClaimResult toClaimResult(StagedFile claimed) {

        return new ClaimResult(
            claimed.handle().value(),
            claimed.size(),
            contentTypeOrNull(claimed),
            claimed.contentHash().getValue());
    }

    /**
     * Parses the claim destination. A blank/absent destination is rejected as {@code InvalidDestination}.
     */
    public static Result<Destination> toDestination(ClaimRequest request) {

        return Destination.of(request == null ? null : request.getDestination());
    }

    private static String contentTypeOrNull(StagedFile file) {

        return file.contentType().isPresent() ? file.contentType().value() : null;
    }
}
