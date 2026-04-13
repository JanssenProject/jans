package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.WorkItem;
import io.jans.shibboleth.model.core.WorkItemState;

import java.util.List;

public class WorkItems {

    private List<WorkItem> items;
    
    public boolean hasAny() {

        return !items.isEmpty();
    }
}