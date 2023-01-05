package io.jans.agama.engine.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

import java.util.Date;

@DataEntry
@ObjectClass(value = FlowRun.ATTR_NAMES.OBJECT_CLASS)
public class FlowRun extends ProtoFlowRun {

    //An enum cannot be used because elements of annotations (like AttributeName) have to be constants
    public class ATTR_NAMES {
        public static final String OBJECT_CLASS = "agmFlowRun";
        public static final String ID = "jansId";
        public static final String STATUS = "agFlowSt";
    }
    
    @AttributeName(name = "agFlowEncCont")
    private String encodedContinuation;

    @AttributeName(name = "jansCustomMessage")
    private String hash;
    
    @AttributeName(name = "exp")
    private Date deletableAt;

/*
    TODO: https://github.com/JanssenProject/jans/issues/1252
    When fixed, AgamaPersistenceService#saveState and getContinuation will need refactoring
    @AttributeName(name = "agFlowCont")
    private byte[] continuation;

    public byte[] getContinuation() {
        return continuation;
    }

    public void setContinuation(byte[] continuation) {
        this.continuation = continuation;
    }
*/  
    public String getEncodedContinuation() {
        return encodedContinuation;
    }

    public void setEncodedContinuation(String encodedContinuation) {
        this.encodedContinuation = encodedContinuation;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Date getDeletableAt() {
        return deletableAt;
    }

    public void setDeletableAt(Date deletableAt) {
        this.deletableAt = deletableAt;
    }

}
