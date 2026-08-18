package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Read view for file-based metadata (`type: FILE`). Exposes the stored file reference.
 */
public class FileMetadataSourceView extends MetadataSourceView {

    @JsonProperty("file_path")
    private final String filePath;

    public FileMetadataSourceView(String filePath) {

        this.filePath = filePath;
    }

    public String getFilePath() {

        return filePath;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return Objects.equals(filePath, ((FileMetadataSourceView) o).filePath);
    }

    @Override
    public int hashCode() {

        return Objects.hash(filePath);
    }

    @Override
    public String toString() {

        return "FileMetadataSourceView{filePath='" + filePath + "'}";
    }
}
