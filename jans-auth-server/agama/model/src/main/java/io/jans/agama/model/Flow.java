package io.jans.agama.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

@DataEntry
@ObjectClass(value = Flow.ATTR_NAMES.OBJECT_CLASS)
public class Flow extends ProtoFlow  {

    //An enum cannot be used because elements of annotations (like AttributeName) have to be constants
    public class ATTR_NAMES { 
        public static final String OBJECT_CLASS = "agmFlow";
        public static final String QNAME = "agFlowQname";
        public static final String META = "agFlowMeta";
        public static final String TRANSPILED = "agFlowTrans";
        public static final String HASH = "jansCustomMessage";
    }
    
    @JsonObject
    @AttributeName(name = ATTR_NAMES.META)
    private FlowMetadata metadata = new FlowMetadata();
    
    @AttributeName(name = "jansScr")
    private String source;

    @AttributeName(name = ATTR_NAMES.TRANSPILED)
    private String transpiled;
    
    @AttributeName(name = "jansScrError")
    private String codeError;

    public FlowMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(FlowMetadata flowMeta) {
        this.metadata = flowMeta;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTranspiled() {
        return transpiled;
    }

    public void setTranspiled(String transpiled) {
        this.transpiled = transpiled;
    }

    public String getCodeError() {
        return codeError;
    }

    public void setCodeError(String codeError) {
        this.codeError = codeError;
    }

}
