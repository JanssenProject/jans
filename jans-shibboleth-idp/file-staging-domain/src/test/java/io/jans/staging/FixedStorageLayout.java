package io.jans.staging;

import io.jans.staging.port.FileStorageLayout;

/**
 * Test layout: a fixed staging directory, and a token-named file whose extension is derived from the
 * content type ({@code xml} for XML-ish types, else {@code bin}).
 */
final class FixedStorageLayout implements FileStorageLayout {

    private final Destination stagingArea;

    FixedStorageLayout(Destination stagingArea) {

        this.stagingArea = stagingArea;
    }

    @Override
    public FileName fileNameFor(Token token, ContentType contentType) {

        String extension = contentType.isPresent() && contentType.getValue().contains("xml") ? "xml" : "bin";
        return FileName.of(token.getValue() + "." + extension).getValue();
    }

    @Override
    public Destination stagingArea() {

        return stagingArea;
    }
}
