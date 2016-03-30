package org.gluu.oxeleven.client;

import org.gluu.oxeleven.model.SignatureAlgorithm;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

/**
 * @author Javier Rojas Blum
 * @version March 29, 2016
 */
public class VerifySignatureRequest extends BaseRequest {

    private String signingInput;
    private String signature;
    private String alias;
    private SignatureAlgorithm signatureAlgorithm;

    public VerifySignatureRequest() {
        setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        setMediaType(MediaType.APPLICATION_FORM_URLENCODED);
        setHttpMethod(HttpMethod.POST);
    }

    public String getSigningInput() {
        return signingInput;
    }

    public void setSigningInput(String signingInput) {
        this.signingInput = signingInput;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }
}
