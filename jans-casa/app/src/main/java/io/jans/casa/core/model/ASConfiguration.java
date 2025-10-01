package io.jans.casa.core.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

@DataEntry
@ObjectClass("jansAppConf")
public class ASConfiguration extends Entry {

    @AttributeName
    private String jansConfDyn;

    @AttributeName
    private String jansConfStatic;

    public String getJansConfStatic() {
        return jansConfStatic;
    }

    public String getJansConfDyn() {
        return jansConfDyn;
    }

    public void setJansConfDyn(String jansConfDyn) {
        this.jansConfDyn = jansConfDyn;
    }

    public void setJansConfStatic(String jansConfStatic) {
        this.jansConfStatic = jansConfStatic;
    }

}
