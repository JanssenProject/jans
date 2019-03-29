/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.io;

import java.io.InputStream;
import java.io.Serializable;

/**
 * @author: Yuriy Movchan Date: 11.21.2010
 */
public class FileUploadWrapper implements Serializable {

    private static final long serialVersionUID = -2202500884190108601L;

    private transient InputStream stream;
    private String fileName;
    private String contentType;
    private Integer fileSize;

    public FileUploadWrapper() {
    }

    public FileUploadWrapper(InputStream stream, String fileName) {
        this(stream, fileName, null, 0);
    }

    public FileUploadWrapper(InputStream stream, String fileName, String contentType, Integer fileSize) {
        this.stream = stream;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    public InputStream getStream() {
        return stream;
    }

    public void setStream(InputStream stream) {
        this.stream = stream;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public String toString() {
        return String.format("FileUploadWrapper [contentType=%s, fileName=%s, fileSize=%s, stream=%s]", contentType,
                fileName, fileSize, stream);
    }

}
