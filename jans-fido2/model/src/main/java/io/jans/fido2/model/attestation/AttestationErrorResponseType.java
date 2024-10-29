package io.jans.fido2.model.attestation;

import io.jans.as.model.error.IErrorType;

public enum AttestationErrorResponseType implements IErrorType {

    /**
     * The request contains a challenge with error.
     */
    INVALID_CHALLENGE("invalid_challenge"),

    /**
     * The request contains unsupported attestation format (fmt)
     */
    UNSUPPORTED_ATTESTATION_FORMAT("unsupported_attestation_format"),

    /**
     * The request is missing a required parameter, includes an
     * invalid parameter value or is otherwise malformed id_session.
     */
    INVALID_SESSION_ID("invalid_session_id"),

    /**
     * The request contains an unsupported register type
     */
    UNSUPPORTED_REGISTER_TYPE("unsupported_register_type"),

    /**
     * Auto enrollment is disabled
     */
    USER_AUTO_ENROLLMENT_IS_DISABLED("user_auto_enrollment_is_disabled"),

    /**
     * Certificate validation error
     */
    INVALID_CERTIFICATE("invalid_certificate"),

    /**
     * Packed validation error
     */
    PACKED_ERROR("packed_error"),

    /**
     * Tpm validation error
     */
    TPM_ERROR("tpm_error"),

    /**
     * Apple validation error
     */
    APPLE_ERROR("apple_error"),

    /**
     * Fido U2F validation error
     */
    FIDO_U2F_ERROR("fido_u2f_error"),

    /**
     * Attestation Origin validation error
     */
    INVALID_ORIGIN("invalid_origin"),
    ;

    private final String paramName;

    AttestationErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    @Override
    public String getParameter() {
        return paramName;
    }

    @Override
    public String toString() {
        return paramName;
    }
}
