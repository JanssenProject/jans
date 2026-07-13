package io.jans.shibboleth.model.core;

public class WorkItem {

    private final Id id; 
    private final WorkItemType type;
    private WorkItemState state;

    private WorkItem(WorkItemType type) {
        
        id = Id.generate();
        this.type = type;
    }

    public Id getId() {

        return id;
    }

    public WorkItemType getType() {

        return type;
    }

    public WorkItemState getState() {

        return state;
    }
}