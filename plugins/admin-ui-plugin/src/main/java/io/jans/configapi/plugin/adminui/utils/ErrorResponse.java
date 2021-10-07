package io.jans.configapi.plugin.adminui.utils;

public enum ErrorResponse {
    GET_ACCESS_TOKEN_ERROR("Error in getting access token."),
    GET_API_PROTECTION_TOKEN_ERROR("Error in getting api protection token."),
    GET_USER_INFO_ERROR("Error in getting User-Info."),
    AUTHORIZATION_CODE_BLANK("Bad Request: Authourization `code` blank or empty."),
    USER_INFO_JWT_BLANK("Bad Request: User-Info jwt is blank or empty."),
    CODE_OR_TOKEN_REQUIRED("Bad Request: Either `code` or `access_token` is required."),
    CHECK_LICENSE_ERROR("Error in checking license status."),
    ACTIVATE_LICENSE_ERROR("Error in activating License."),
    GET_LICENSE_DETAILS_ERROR("Error in fetching license details."),
    UPDATE_LICENSE_DETAILS_ERROR("Problem in updating license details"),
    LICENSE_VALIDITY_PERIOD_NOT_FOUND("Bad Request: License Validity Period not found in request."),
    INVALID_MAXIMUM_ACTIVATIONS("Bad Request: License Maximum Activations cannot be less than 1."),
    AUDIT_LOGGING_ERROR("Error in audit logging"),
    ERROR_READING_CONFIG("Error in reading auiConfiguration");

    private final String description;

    ErrorResponse(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "description='" + description + '\'' +
                '}';
    }
}
