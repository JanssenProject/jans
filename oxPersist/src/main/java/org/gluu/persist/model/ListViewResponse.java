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
public class ListViewResponse<T> implements Serializable {

    private static final long serialVersionUID = -4211997747213144092L;

    private int startIndex;

    // TODO: Rename to totalEntriesCount
    private int totalResults;
    
    // TODO: Rename to entriesCount
    private int itemsPerPage;

    // TODO: Rename to entries
    private List<T> result;

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public final List<T> getResult() {
        return result;
    }

    public final void setResult(List<T> result) {
        this.result = result;
    }

}
