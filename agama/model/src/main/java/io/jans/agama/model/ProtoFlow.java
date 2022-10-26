package io.jans.agama.model;

/**
 * This class is used as a vehicle to overcome jans-orm limitation related to data 
 * destruction when an update is made on a partially retrieved entity. It also helps
 * to make "lighter" retrievals of Flows
 */
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

@DataEntry
@ObjectClass(value = Flow.ATTR_NAMES.OBJECT_CLASS)
public class ProtoFlow extends Entry {
    
    @AttributeName(name = Flow.ATTR_NAMES.QNAME)
    private String qname;
    
    @AttributeName(name = Flow.ATTR_NAMES.HASH)
    private String transHash;
    
    @AttributeName(name = "jansRevision")
    private int revision;

    @AttributeName(name = "jansEnabled")
    private boolean enabled;

    public String getQname() {
        return qname;
    }

    public void setQname(String qname) {
        this.qname = qname;
    }

    public String getTransHash() {
        return transHash;
    }

    public void setTransHash(String transHash) {
        this.transHash = transHash;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }
    
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
}
