package io.jans.agama.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

@DataEntry
@ObjectClass(value = "agmConfig")
public class Config extends Entry {
    
    @AttributeName
    private String ou;

    @AttributeName(name = "jansScr")
    private String utilScript;

    @JsonObject
    @AttributeName(name = "jansConfApp")
    private EngineConfig engineConf;

    public String getOu() {
        return ou;
    }
    
    public void setOu(String ou) {
        this.ou = ou;
    }

    public String getUtilScript() {
        return utilScript;
    }

    public void setUtilScript(String utilScript) {
        this.utilScript = utilScript;
    }

    public EngineConfig getEngineConf() {
        return engineConf;
    }

    public void setEngineConf(EngineConfig engineConf) {
        this.engineConf = engineConf;
    }

}
