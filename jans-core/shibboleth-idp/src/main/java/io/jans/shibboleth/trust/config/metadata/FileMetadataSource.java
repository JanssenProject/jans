package io.jans.shibboleth.trust.config.metadata;

import java.nio.file.Path;
import java.util.Objects;

import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

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

    public static Result<MetadataSource> of(String filePath) {

        if (filePath == null || filePath.isBlank() ) {

            return Result.failure(RequiredValueMissing.forField("filePath"));
        }

        return Result.success(new FileMetadataSource(filePath));
    }
}
