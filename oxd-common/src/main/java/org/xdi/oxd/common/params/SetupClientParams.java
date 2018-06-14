package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/03/2017
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class SetupClientParams extends RegisterSiteParams {

    @JsonProperty(value = "setup_client_name")
    private String setupClientName;

    public String getSetupClientName() {
        return setupClientName;
    }

    public void setSetupClientName(String setupClientName) {
        this.setupClientName = setupClientName;
    }
}
