package io.jans.ca.plugin.adminui.utils;

public enum ErrorResponse {
    GET_ACCESS_TOKEN_ERROR("Error in getting access token."),
    GET_API_PROTECTION_TOKEN_ERROR("Error in generating token to access Jans Config Api endpoints."),
    GET_USER_INFO_ERROR("Error in getting User-Info."),
    AUTHORIZATION_CODE_BLANK("Bad Request: Authourization `code` blank or empty."),
    USER_INFO_JWT_BLANK("User-Info jwt is blank or empty. Generating token with default scopes."),
    CODE_OR_TOKEN_REQUIRED("Bad Request: Either `code` or `access_token` is required."),
    CODE_VERIFIER_REQUIRED("Bad Request: `code_verifier` is required."),
    CHECK_LICENSE_ERROR("Error in checking license status. Check logs for further details."),
    ERROR_IN_LICENSE_CONFIGURATION_VALIDATION("Error in validating license configuration."),
    ACTIVATE_LICENSE_ERROR("Error in activating License. Check logs for further details."),
    ERROR_IN_TRIAL_LICENSE("Error in generating trial license. Check logs for further details."),
    GET_LICENSE_DETAILS_ERROR("Error in fetching license details. Check logs for further details."),
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
    ADMIUI_ROLE_ALREADY_PRESENT("Admin UI role already present."),
    EDIT_ADMIUI_ROLES_ERROR("Error in editing Admin UI roles."),
    DELETE_ADMIUI_ROLES_ERROR("Error in deleting Admin UI roles."),
    GET_ADMIUI_PERMISSIONS_ERROR("Error in fetching Admin UI permissions."),
    SAVE_ADMIUI_PERMISSIONS_ERROR("Error in saving Admin UI permissions."),
    ADMIUI_PERMISSIONS_ALREADY_PRESENT("Permission already present."),
    EDIT_ADMIUI_PERMISSIONS_ERROR("Error in editing Admin UI permissions."),
    DELETE_ADMIUI_PERMISSIONS_ERROR("Error in deleting Admin UI permissions."),
    ROLE_MARKED_UNDELETABLE("Role cannot be deleted. Please set ‘deletable’ property of role to true."),
    UNABLE_TO_DELETE_ROLE_MAPPED_TO_PERMISSIONS("Role is mapped to permissions so cannot be deleted. Please remove the permissions mapped before deleting the role."),
    UNABLE_TO_DELETE_PERMISSION_MAPPED_TO_ROLE("Permission is mapped to role so cannot be deleted. Please remove the permission mapped to the role before deleting it."),
    ERROR_IN_READING_CONFIGURATION("Error in reading Admin UI configuration."),
    TOKEN_GENERATION_ERROR("Error in generating SCAN API access token."),
    ERROR_IN_SAVING_LICENSE_CLIENT("There is an error when attempting to save the OpenID client configuration for accessing the license APIs in a persistence layer."),
    RETRIEVE_LICENSE_ERROR("Error in retrieving license. Check logs for further details."),
    BLANK_JWT("JWT is blank or empty"),
    ISS_CLAIM_NOT_FOUND("ISS claim not fount in jwt"),
    ERROR_IN_DCR("Error in DCR using SSA."),
    LICENSE_NOT_PRESENT("Active license not present."),
    LICENSE_ALREADY_ACTIVE("The license has been already activated."),
    LICENSE_CONFIG_ABSENT("License configuration is not present."),
    SCAN_HOSTNAME_MISSING("SCAN api hostname is missing in configuration.")
    ;

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
