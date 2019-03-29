package org.gluu.config.oxtrust;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScimProperties implements Serializable {

    /**
    *
    */
    private static final long serialVersionUID = -5154249316054593386L;

    private int maxCount;

    public int getMaxCount() {
        return this.maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

}
