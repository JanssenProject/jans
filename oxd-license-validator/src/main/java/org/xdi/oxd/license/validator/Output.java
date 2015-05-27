package org.xdi.oxd.license.validator;

import org.codehaus.jackson.annotate.JsonProperty;
import org.xdi.oxd.license.client.js.LicenseMetadata;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/05/2015
 */

public class Output implements Serializable {
    @JsonProperty(value = "valid")
    private boolean valid = false;

    @JsonProperty(value = "metadata")
    private org.xdi.oxd.license.client.js.LicenseMetadata metadata = null;

    public LicenseMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(LicenseMetadata metadata) {
        this.metadata = metadata;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
