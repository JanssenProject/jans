/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util.io;

import java.io.InputStream;
import java.util.Date;

/**
 * @author: Yuriy Movchan Date: 11.20.2010
 */
public class DownloadWrapper {
    private InputStream stream;
    private String name;
    private String contentType;
    private Date modificationDate;
    private int contentLength;

    public DownloadWrapper(InputStream stream, String name, String contentType, Date modificationDate) {
        this(stream, name, contentType, modificationDate, 0);
    }

    public DownloadWrapper(InputStream stream, String name, String contentType, Date modificationDate,
            int contentLength) {
        this.stream = stream;
        this.name = name;
        this.contentType = contentType;
        this.modificationDate = modificationDate != null ? new Date(modificationDate.getTime()) : null;
        this.contentLength = contentLength;
    }

    public InputStream getStream() {
        return stream;
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public Date getModificationDate() {
        return modificationDate != null ? new Date(modificationDate.getTime()) : null;
    }

    public void setModificationDate(Date modificationDate) {
        this.modificationDate = modificationDate != null ? new Date(modificationDate.getTime()) : null;
    }

    public boolean isReady() {
        return (stream != null) && (name != null) && (contentType != null) && (modificationDate != null)
                && (contentLength >= 0);
    }

    @Override
    public String toString() {
        return String.format(
                "FileDownloadWrapper [contentLength=%s, contentType=%s, modificationDate=%s, name=%s, stream=%s]",
                contentLength, contentType, modificationDate, name, stream);
    }

}
