package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/06/2016
 */

public class RpGetGatResponse implements IOpResponse {

    @JsonProperty(value = "gat")
    private String gat;

    public RpGetGatResponse() {
    }

    public String getGat() {
        return gat;
    }

    public void setGat(String gat) {
        this.gat = gat;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RpGetGatResponse");
        sb.append("{gat='").append(gat).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
