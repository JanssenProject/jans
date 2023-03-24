package io.jans.agama.engine.model;

/**
 * This class is used as a vehicle to overcome jans-orm limitation related to data 
 * destruction when an update is made on a partially retrieved entity. It also helps
 * to make "lighter" retrievals of FlowRuns
 */
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

@DataEntry
@ObjectClass(value = FlowRun.ATTR_NAMES.OBJECT_CLASS)
public class ProtoFlowRun extends Entry {
    
    @AttributeName(name = FlowRun.ATTR_NAMES.ID)
    private String id;
    
    @JsonObject
    @AttributeName(name = FlowRun.ATTR_NAMES.STATUS)
    private FlowStatus status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FlowStatus getStatus() {
        return status;
    }

    public void setStatus(FlowStatus status) {
        this.status = status;
    }
    
}
