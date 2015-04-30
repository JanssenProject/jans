package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class ObtainRptParams implements IParams {

    @JsonProperty(value = "aat_token")
    private String aat;
    @JsonProperty(value = "am_host")
    private String amHost;

    public ObtainRptParams() {
    }

    public String getAat() {
        return aat;
    }

    public void setAat(String p_aat) {
        aat = p_aat;
    }

    public String getAmHost() {
        return amHost;
    }

    public void setAmHost(String p_amHost) {
        amHost = p_amHost;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ObtainRptParams");
        sb.append("{aat='").append(aat).append('\'');
        sb.append(", amHost='").append(amHost).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
