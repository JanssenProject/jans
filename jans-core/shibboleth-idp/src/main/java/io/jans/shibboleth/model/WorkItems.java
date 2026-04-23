package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.WorkItem;
import io.jans.shibboleth.model.core.WorkItemState;

import java.util.List;

public class WorkItems {

    private List<WorkItem> items;

    private WorkItems(List<WorkItem> items) {

        this.items = items != null ? List.copyOf(items) : List.of();
    }
    
    public boolean hasAny() {

        return !items.isEmpty();
    }

    public static WorkItems empty() {

        return new WorkItems(null);
    }
}