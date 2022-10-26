package io.jans.ca.plugin.adminui.utils;

public enum ErrorResponse {
    GET_ACCESS_TOKEN_ERROR("Error in getting access token."),
    GET_API_PROTECTION_TOKEN_ERROR("Error in getting api protection token."),
    GET_USER_INFO_ERROR("Error in getting User-Info."),
    AUTHORIZATION_CODE_BLANK("Bad Request: Authourization `code` blank or empty."),
    USER_INFO_JWT_BLANK("User-Info jwt is blank or empty. Generating token with default scopes."),
    CODE_OR_TOKEN_REQUIRED("Bad Request: Either `code` or `access_token` is required."),
    CHECK_LICENSE_ERROR("Error in checking license status."),
    SAVE_LICENSE_SPRING_CREDENTIALS_ERROR("Error in saving license spring credentials."),
    ACTIVATE_LICENSE_ERROR("Error in activating License."),
    GET_LICENSE_DETAILS_ERROR("Error in fetching license details."),
    UPDATE_LICENSE_DETAILS_ERROR("Problem in updating license details"),
    LICENSE_VALIDITY_PERIOD_NOT_FOUND("Bad Request: License Validity Period not found in request."),
    INVALID_MAXIMUM_ACTIVATIONS("Bad Request: License Maximum Activations cannot be less than 1."),
    AUDIT_LOGGING_ERROR("Error in audit logging"),
    ERROR_READING_CONFIG("Error in reading auiConfiguration"),
    ERROR_READING_ROLE_PERMISSION_MAP("Error in reading role-permissions mapping from Auth Server."),
    ROLE_PERMISSION_MAP_NOT_FOUND("Role-permissions mapping not found."),
    ROLE_NOT_FOUND("Bad Request: Admin UI Role not found in Auth Server."),
    PERMISSION_NOT_FOUND("Bad Request: Admin UI permission not found in Auth Server."),
    ERROR_IN_MAPPING_ROLE_PERMISSION("Error in mapping role-permission."),
    ERROR_IN_DELETING_ROLE_PERMISSION("Error in deleting role-permission."),
    ROLE_PERMISSION_MAPPING_PRESENT("Role permission mapping already present. Please use HTTP PUT request to modify mapping."),
    GET_ADMIUI_ROLES_ERROR("Error in fetching Admin UI roles."),
    SAVE_ADMIUI_ROLES_ERROR("Error in saving Admin UI roles."),
    EDIT_ADMIUI_ROLES_ERROR("Error in editing Admin UI roles."),
    DELETE_ADMIUI_ROLES_ERROR("Error in deleting Admin UI roles."),
    GET_ADMIUI_PERMISSIONS_ERROR("Error in fetching Admin UI permissions."),
    SAVE_ADMIUI_PERMISSIONS_ERROR("Error in saving Admin UI permissions."),
    EDIT_ADMIUI_PERMISSIONS_ERROR("Error in editing Admin UI permissions."),
    DELETE_ADMIUI_PERMISSIONS_ERROR("Error in deleting Admin UI permissions."),
    ROLE_MARKED_UNDELETABLE("Role cannot be deleted. Please set ‘deletable’ property of role to true."),
    UNABLE_TO_DELETE_ROLE_MAPPED_TO_PERMISSIONS("Role is mapped to permissions so cannot be deleted. Please remove the permissions mapped before deleting the role."),
    UNABLE_TO_DELETE_PERMISSION_MAPPED_TO_ROLE("Permission is mapped to role so cannot be deleted. Please remove the permission mapped to the role before deleting it.");

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
