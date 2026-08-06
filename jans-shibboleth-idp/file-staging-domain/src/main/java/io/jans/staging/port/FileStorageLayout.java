package io.jans.staging.port;

import io.jans.staging.ContentType;
import io.jans.staging.Destination;
import io.jans.staging.FileName;
import io.jans.staging.Token;

/**
 * The file-layout policy: how staged files are named and where the staging area lives. The service
 * asks it for a filename (token-based for idempotency, with an extension chosen from the content
 * type) and for the staging directory, then builds explicit storage paths from those. Keeping this a
 * port means the media-type to extension mapping and the staging location are the adapter's concern,
 * not hardcoded in the domain.
 */
public interface FileStorageLayout {

    FileName fileNameFor(Token token, ContentType contentType);

    Destination stagingArea();
}
