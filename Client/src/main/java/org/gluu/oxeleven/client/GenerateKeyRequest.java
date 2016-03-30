package org.gluu.oxeleven.client;

import org.gluu.oxeleven.model.SignatureAlgorithm;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

/**
 * @author Javier Rojas Blum
 * @version March 29, 2016
 */
public class GenerateKeyRequest extends BaseRequest {

    private SignatureAlgorithm signatureAlgorithm;

    public GenerateKeyRequest() {
        setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        setMediaType(MediaType.APPLICATION_FORM_URLENCODED);
        setHttpMethod(HttpMethod.POST);
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }
}
