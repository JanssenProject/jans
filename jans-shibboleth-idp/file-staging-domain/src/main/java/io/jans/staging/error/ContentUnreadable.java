package io.jans.staging.error;

/**
 * The uploaded content stream could not be read, or could not be written to storage — an I/O failure
 * while staging. Maps to HTTP 500.
 */
public final class ContentUnreadable extends StagingError {

    private ContentUnreadable() {

        super("The content stream could not be read or stored");
    }

    public static ContentUnreadable instance() {

        return new ContentUnreadable();
    }
}
