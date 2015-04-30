package org.xdi.oxd.license.client.data;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/11/2014
 */

public enum ErrorType {
    LICENSE_ID_EMPTY("empty_license_id", "license_id parameter is not set"),
    LICENSE_ID_INVALID("invalid_license_id", "license_id is invalid (expired or does not exist on license server)"),
    CSR_EMPTY("empty_csr", "CSR is empty or otherwise invalid."),
    EJB_CA_FAILED_TO_SIGN_CSR("ejb_ca_failed_to_sign_csr", "EJB CA failed to sign csr.");

    private String error;
    private String errorDescription;

    private ErrorType(String error, String errorDescription) {
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}
