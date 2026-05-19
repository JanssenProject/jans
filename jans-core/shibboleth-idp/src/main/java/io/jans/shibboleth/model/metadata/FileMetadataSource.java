package io.jans.shibboleth.model.metadata;

import java.nio.file.Path;

import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.util.TrustResult;

public class FileMetadataSource implements MetadataSource {

    private final String filePath;

    private FileMetadataSource(String filePath) {

        this.filePath = filePath;
    }

    @Override
    public MetadataSourceType getType() {

        return MetadataSourceType.FILE;
    }

    public static TrustResult<MetadataSource> of(String filePath) {

        if (filePath == null || filePath.isBlank() ) {

            return TrustResult.failure(new CannotBeNullOrBlank("filePath"));
        }

        return TrustResult.success(new FileMetadataSource(filePath));
    }
}
