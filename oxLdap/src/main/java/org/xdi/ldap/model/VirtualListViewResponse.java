package org.xdi.ldap.model;

import java.io.Serializable;

/**
 * @author Val Pecaoco
 */
public class VirtualListViewResponse implements Serializable {

    private int totalResults;
    private int itemsPerPage;
    private int startIndex;

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
}
