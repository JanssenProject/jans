/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.persist.model;

import java.io.Serializable;
import java.util.List;

/**
 * @author Val Pecaoco
 * @author Yuriy Movchan Date: 01/25/2018
 */
public class PagedResult<T> implements Serializable {

    private static final long serialVersionUID = -4211997747213144092L;

    private int start;

    private int totalEntriesCount;

    private int entriesCount;

    private List<T> entries;

    public int getTotalEntriesCount() {
        return totalEntriesCount;
    }

    public void setTotalEntriesCount(int totalEntriesCount) {
        this.totalEntriesCount = totalEntriesCount;
    }

    public int getEntriesCount() {
        return entriesCount;
    }

    public void setEntriesCount(int entriesCount) {
        this.entriesCount = entriesCount;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public final List<T> getEntries() {
        return entries;
    }

    public final void setEntries(List<T> entries) {
        this.entries = entries;
    }

}
