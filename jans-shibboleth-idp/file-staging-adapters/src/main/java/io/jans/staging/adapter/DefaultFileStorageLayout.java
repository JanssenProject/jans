package io.jans.staging.adapter;

import io.jans.staging.ContentType;
import io.jans.staging.Destination;
import io.jans.staging.FileName;
import io.jans.staging.Token;
import io.jans.staging.port.FileStorageLayout;

import java.util.Map;

/**
 * Default {@link FileStorageLayout}: files are named {@code <token>.<ext>} where the extension is
 * looked up from the content type (falling back to a configured default), and staged under a fixed
 * directory. The token base keeps a claim idempotent; the content-type → extension map and staging
 * directory are deployment configuration, owned here rather than in the domain.
 */
public final class DefaultFileStorageLayout implements FileStorageLayout {

    private final Destination stagingArea;
    private final Map<String, String> extensionByContentType;
    private final String defaultExtension;

    public DefaultFileStorageLayout(Destination stagingArea, Map<String, String> extensionByContentType,
                                    String defaultExtension) {

        this.stagingArea = stagingArea;
        this.extensionByContentType = Map.copyOf(extensionByContentType);
        this.defaultExtension = defaultExtension;
    }

    /**
     * A layout with the common XML metadata mappings and a {@code bin} fallback.
     */
    public static DefaultFileStorageLayout withDefaults(Destination stagingArea) {

        Map<String, String> defaults = Map.of(
            "application/samlmetadata+xml", "xml",
            "application/xml", "xml",
            "text/xml", "xml");
        return new DefaultFileStorageLayout(stagingArea, defaults, "bin");
    }

    @Override
    public FileName fileNameFor(Token token, ContentType contentType) {

        String extension = extensionFor(contentType);
        String name = extension.isEmpty() ? token.getValue() : token.getValue() + "." + extension;
        return FileName.of(name).getValue();
    }

    @Override
    public Destination stagingArea() {

        return stagingArea;
    }

    private String extensionFor(ContentType contentType) {

        if (!contentType.isPresent()) {

            return defaultExtension;
        }
        return extensionByContentType.getOrDefault(contentType.getValue(), defaultExtension);
    }
}
