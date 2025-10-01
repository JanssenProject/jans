package io.jans.configapi.core.util;

public enum ApiErrorResponse {
    GENERAL_ERROR("%s."),
    MISSING_ATTRIBUTES("Attributes Missing in request are {%s}."),
    SAME_NAME_USER_EXISTS_ERROR("User with same name {%s} already exists!"),
    SAME_NAME_EMAIL_EXISTS_ERROR("User with same email {%s} already exists!"),
    FETCH_DATA_ERROR("Error in fetching data."),
    CREATE_USER_ERROR("Error in creatig user."),
    UPDATE_USER_ERROR("Error in updating user.")
    ;

    private final String description;

    ApiErrorResponse(String description) {
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
