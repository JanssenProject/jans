package io.jans.shibboleth.model.metadata;

import java.nio.file.Path;
import java.util.Objects;

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

    public String getFilePath() {

        return filePath;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        
        if (o == null || getClass() != o.getClass()) return false;

        FileMetadataSource other = (FileMetadataSource) o;

        return Objects.equals(filePath,other.filePath);
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(filePath);
    }

    public static TrustResult<MetadataSource> of(String filePath) {

        if (filePath == null || filePath.isBlank() ) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("filePath"));
        }

        return TrustResult.success(new FileMetadataSource(filePath));
    }
}
