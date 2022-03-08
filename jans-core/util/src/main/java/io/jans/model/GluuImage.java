/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Transient;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.Arrays;
import java.util.Date;


/**
 * Hold Image specific information
 *
 * @author Yuriy Movchan Date: 11.03.2010
 */
@XmlRootElement
@JsonIgnoreProperties({ "sourceFilePath", "thumbFilePath", "storeTemporary", "logo", "landscape" })
public class GluuImage implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private String uuid;

    private String creator;

    private String sourceName;

    private Date creationDate;

    private String sourceContentType;

    private String sourceFilePath;

    private long size;

    private int width;

    private int height;

    private String thumbContentType;

    private int thumbWidth;

    private int thumbHeight;

    private byte[] data;
    private byte[] thumbData;

    private String thumbFilePath;

    private boolean storeTemporary;

    private boolean logo;

    @Transient
    @XmlTransient
    public boolean isLandscape() {
        return width > height;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public void setSourceFilePath(String sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
    }

    public Date getCreationDate() {
        return creationDate != null ? new Date(creationDate.getTime()) : null;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate != null ? new Date(creationDate.getTime()) : null;
    }

    public String getSourceContentType() {
        return sourceContentType;
    }

    public void setSourceContentType(String sourceContentType) {
        this.sourceContentType = sourceContentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getThumbFilePath() {
        return thumbFilePath;
    }

    public void setThumbFilePath(String thumbFilePath) {
        this.thumbFilePath = thumbFilePath;
    }

    public String getThumbContentType() {
        return thumbContentType;
    }

    public void setThumbContentType(String thumbContentType) {
        this.thumbContentType = thumbContentType;
    }

    public int getThumbWidth() {
        return thumbWidth;
    }

    public void setThumbWidth(int thumbWidth) {
        this.thumbWidth = thumbWidth;
    }

    public int getThumbHeight() {
        return thumbHeight;
    }

    public void setThumbHeight(int thumbHeight) {
        this.thumbHeight = thumbHeight;
    }

    public void setStoreTemporary(boolean storeTemporary) {
        this.storeTemporary = storeTemporary;
    }

    @Transient
    @XmlTransient
    public boolean isStoreTemporary() {
        return storeTemporary;
    }

    public void setLogo(boolean logo) {
        this.logo = logo;
    }

    @Transient
    @XmlTransient
    public boolean isLogo() {
        return logo;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Transient
    @XmlTransient
    public byte[] getData() {
        return data;
    }

    public byte[] getThumbData() {
        return thumbData;
    }

    public void setThumbData(byte[] thumbData) {
        this.thumbData = thumbData;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GluuImage other = (GluuImage) obj;
        if (uuid == null) {
            if (other.uuid != null) {
                return false;
            }
        } else if (!uuid.equals(other.uuid)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GluuImage [uuid=").append(uuid).append(", creator=").append(creator).append(", sourceName=")
                .append(sourceName).append(", creationDate=").append(creationDate).append(", sourceContentType=")
                .append(sourceContentType).append(", sourceFilePath=").append(sourceFilePath).append(", size=")
                .append(size).append(", width=").append(width).append(", height=").append(height)
                .append(", thumbContentType=").append(thumbContentType).append(", thumbWidth=").append(thumbWidth)
                .append(", thumbHeight=").append(thumbHeight).append(", data=").append(Arrays.toString(data))
                .append(", thumbData=").append(Arrays.toString(thumbData)).append(", thumbFilePath=")
                .append(thumbFilePath).append(", storeTemporary=").append(storeTemporary).append(", logo=").append(logo)
                .append("]");
        return builder.toString();
    }
}
