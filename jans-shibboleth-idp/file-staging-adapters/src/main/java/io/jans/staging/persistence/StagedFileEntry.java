package io.jans.staging.persistence;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

import java.util.Date;

/**
 * jans-orm storage entry for a staged file's <b>metadata</b> (the bytes live in the jans document store,
 * not here). Object class {@code jansStagedFile}, under {@code ou=stagedFiles,o=jans}. The primary key is
 * the DN {@code inum=<token>,ou=stagedFiles,o=jans}, where {@code inum} is the opaque staging token — the
 * file's identity. Timestamps are stored as native timestamps, letting jans-orm own the date codec.
 */
@DataEntry(sortBy = "jansStagedAt", sortByName = "jansStagedAt")
@ObjectClass("jansStagedFile")
public class StagedFileEntry extends BaseEntry {

    private static final long serialVersionUID = 1L;

    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "jansFileName")
    private String fileName;

    @AttributeName(name = "jansContentHash")
    private String contentHash;

    @AttributeName(name = "jansContentSize")
    private long size;

    @AttributeName(name = "jansContentType")
    private String contentType;

    @AttributeName(name = "jansStagedFileStatus")
    private String status;

    @AttributeName(name = "jansStagedAt")
    private Date stagedAt;

    @AttributeName(name = "jansExpiresAt")
    private Date expiresAt;

    @AttributeName(name = "jansHandle")
    private String handle;

    public String getInum() {

        return inum;
    }

    public void setInum(String inum) {

        this.inum = inum;
    }

    public String getFileName() {

        return fileName;
    }

    public void setFileName(String fileName) {

        this.fileName = fileName;
    }

    public String getContentHash() {

        return contentHash;
    }

    public void setContentHash(String contentHash) {

        this.contentHash = contentHash;
    }

    public long getSize() {

        return size;
    }

    public void setSize(long size) {

        this.size = size;
    }

    public String getContentType() {

        return contentType;
    }

    public void setContentType(String contentType) {

        this.contentType = contentType;
    }

    public String getStatus() {

        return status;
    }

    public void setStatus(String status) {

        this.status = status;
    }

    public Date getStagedAt() {

        return stagedAt;
    }

    public void setStagedAt(Date stagedAt) {

        this.stagedAt = stagedAt;
    }

    public Date getExpiresAt() {

        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {

        this.expiresAt = expiresAt;
    }

    public String getHandle() {

        return handle;
    }

    public void setHandle(String handle) {

        this.handle = handle;
    }
}
