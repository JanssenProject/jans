package org.xdi.oxd.license.client.data;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/11/2014
 */

public class CertificateGrantResponse implements Serializable {

    @JsonProperty(value = "expires_at")
    private Date expiresAt;

    public CertificateGrantResponse() {
    }

    public CertificateGrantResponse(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }
}
