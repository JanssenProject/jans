/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.external;

import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.scim.ScimType;
import io.jans.orm.model.base.Entry;
import io.jans.orm.model.PagedResult;
import io.jans.scim.model.GluuGroup;
import io.jans.scim.model.scim.ScimCustomPerson;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.service.custom.script.ExternalScriptService;

/**
 * @author Val Pecaoco
 * @author jgomer
 */
@ApplicationScoped
public class ExternalScimService extends ExternalScriptService {

	private static final long serialVersionUID = 1767751544454591666L;

    public ExternalScimService() {
        super(CustomScriptType.SCIM);
    }

    private CustomScriptConfiguration findConfigWithGEVersion(int version) {
        return customScriptConfigurations.stream()
                .filter(sc -> executeExternalGetApiVersion(sc) >= version)
                .findFirst().orElse(null);    
    }
    
    private void logAndSave(CustomScriptConfiguration customScriptConfiguration, Exception e) {
        log.error(e.getMessage(), e);
        saveScriptError(customScriptConfiguration.getCustomScript(), e);        
    }

    private boolean executeScimCreateUserMethod(ScimCustomPerson user, CustomScriptConfiguration customScriptConfiguration) {

        try {
            log.debug("Executing python 'SCIM Create User' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.createUser(user, configurationAttributes);
            log.debug("executeScimCreateUserMethod result = " + result);
            return result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;

    }

    private boolean executeScimPostCreateUserMethod(ScimCustomPerson user, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 2)
                return true;

            log.debug("Executing python 'SCIM Post Create User' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.postCreateUser(user, configurationAttributes);
            log.debug("executeScimPostCreateUserMethod result = " + result);
            return result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;

    }

    private boolean executeScimUpdateUserMethod(ScimCustomPerson user, CustomScriptConfiguration customScriptConfiguration) {

        try {
            log.debug("Executing python 'SCIM Update User' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.updateUser(user, configurationAttributes);
            log.debug("executeScimUpdateUserMethod result = " + result);
            return result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;

    }

    private boolean executeScimPostUpdateUserMethod(ScimCustomPerson user, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 2)
                return true;

            log.debug("Executing python 'SCIM Post Update User' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.postUpdateUser(user, configurationAttributes);
            log.debug("executeScimPostUpdateUserMethod result = " + result);
            return result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;

    }

    private boolean executeScimDeleteUserMethod(ScimCustomPerson user, CustomScriptConfiguration customScriptConfiguration) {

        try {
            log.debug("Executing python 'SCIM Delete User' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.deleteUser(user, configurationAttributes);
            log.debug("executeScimDeleteUserMethod result = " + result);
            return result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;

    }

    private boolean executeScimPostDeleteUserMethod(ScimCustomPerson user, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 2)
                return true;

            log.debug("Executing python 'SCIM Post Delete User' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.postDeleteUser(user, configurationAttributes);
            log.debug("executeScimPostDeleteUserMethod result = " + result);
            return result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;

    }

    private boolean executeScimGetUserMethod(ScimCustomPerson user, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 3)
                return true;

            log.debug("Executing python 'SCIM Get User' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.getUser(user, configurationAttributes);
            log.debug("executeScimGetUserMethod result = " + result);
            return result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;

    }

    private boolean executeScimCreateGroupMethod(GluuGroup group, CustomScriptConfiguration customScriptConfiguration) {

        try {
            log.debug("Executing python 'SCIM Create Group' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.createGroup(group, configurationAttributes);
            log.debug("executeScimCreateGroupMethod result = " + result);
            return result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;

    }

    private boolean executeScimPostCreateGroupMethod(GluuGroup group, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 2)
                return true;

            log.debug("Executing python 'SCIM Post Create Group' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.postCreateGroup(group, configurationAttributes);
            log.debug("executeScimPostCreateGroupMethod result = " + result);
            return result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;

    }

    private boolean executeScimUpdateGroupMethod(GluuGroup group, CustomScriptConfiguration customScriptConfiguration) {

        try {
            log.debug("Executing python 'SCIM Update Group' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.updateGroup(group, configurationAttributes);
            log.debug("executeScimUpdateGroupMethod result = " + result);
            return  result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;

    }

    private boolean executeScimPostUpdateGroupMethod(GluuGroup group, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 2)
                return true;

            log.debug("Executing python 'SCIM Post Update Group' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.postUpdateGroup(group, configurationAttributes);
            log.debug("executeScimPostUpdateGroupMethod result = " + result);
            return  result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;

    }

    private boolean executeScimDeleteGroupMethod(GluuGroup group, CustomScriptConfiguration customScriptConfiguration) {

        try {
            log.debug("Executing python 'SCIM Delete Group' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.deleteGroup(group, configurationAttributes);
            log.debug("executeScimDeleteGroupMethod result = " + result);
            return  result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;

    }

    private boolean executeScimPostDeleteGroupMethod(GluuGroup group, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 2)
                return true;

            log.debug("Executing python 'SCIM Post Delete Group' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.postDeleteGroup(group, configurationAttributes);
            log.debug("executeScimPostDeleteGroupMethod result = " + result);
            return  result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;

    }

    private boolean executeScimGetGroupMethod(GluuGroup group, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 3)
                return true;

            log.debug("Executing python 'SCIM Get Group' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.getGroup(group, configurationAttributes);
            log.debug("executeScimGetGroupMethod result = " + result);
            return result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;

    }

    private boolean executeScimPostSearchUsersMethod(PagedResult<ScimCustomPerson> pagedResult, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 4)
                return true;
            
            log.debug("Executing python 'SCIM Search Users' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            
            boolean result = externalType.postSearchUsers(pagedResult, configurationAttributes);
            log.debug("executeScimPostSearchUsersMethod result = " + result);
            return result;

        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;
        
    }
    
    private boolean executeScimPostSearchGroupsMethod(PagedResult<GluuGroup> pagedResult, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 4)
                return true;
            
            log.debug("Executing python 'SCIM Search Groups' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            
            boolean result = externalType.postSearchGroups(pagedResult, configurationAttributes);
            log.debug("executeScimPostSearchGroupsMethod result = " + result);
            return result;
            
        } catch (Exception e) {
            logAndSave(customScriptConfiguration, e);
        }
        return false;
        
    }
    
    public boolean executeScimCreateUserMethods(ScimCustomPerson user) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimCreateUserMethod(user, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimPostCreateUserMethods(ScimCustomPerson user) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostCreateUserMethod(user, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimUpdateUserMethods(ScimCustomPerson user) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimUpdateUserMethod(user, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimPostUpdateUserMethods(ScimCustomPerson user) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostUpdateUserMethod(user, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimDeleteUserMethods(ScimCustomPerson user) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimDeleteUserMethod(user, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimPostDeleteUserMethods(ScimCustomPerson user) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostDeleteUserMethod(user, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimCreateGroupMethods(GluuGroup group) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimCreateGroupMethod(group, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimPostCreateGroupMethods(GluuGroup group) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostCreateGroupMethod(group, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimUpdateGroupMethods(GluuGroup group) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimUpdateGroupMethod(group, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimPostUpdateGroupMethods(GluuGroup group) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostUpdateGroupMethod(group, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimDeleteGroupMethods(GluuGroup group) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimDeleteGroupMethod(group, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimPostDeleteGroupMethods(GluuGroup group) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostDeleteGroupMethod(group, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimGetUserMethods(ScimCustomPerson user) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimGetUserMethod(user, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimGetGroupMethods(GluuGroup group) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimGetGroupMethod(group, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimPostSearchUsersMethods(PagedResult<ScimCustomPerson> result) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostSearchUsersMethod(result, customScriptConfiguration)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean executeScimPostSearchGroupsMethods(PagedResult<GluuGroup> result) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostSearchGroupsMethod(result, customScriptConfiguration)) {
                return false;
            }
        }
        return true;
    }

    public Response executeManageResourceOperation(Entry entity, Object payload, OperationContext context) throws Exception {
        
        CustomScriptConfiguration configuration = findConfigWithGEVersion(5);
        Response result = null;
        
        try {
            if (configuration != null) {
                log.debug("Executing python 'SCIM Manage Resource Operation' method");
                ScimType externalType = (ScimType) configuration.getExternalType();
                Map<String, SimpleCustomProperty> configurationAttributes = configuration.getConfigurationAttributes();

                result = externalType.manageResourceOperation(context, entity, payload, configurationAttributes);
                log.debug("executeManageResourceOperation result HTTP code = {}", 
                    Optional.ofNullable(result).map(res -> Integer.toString(res.getStatus())).orElse("none"));
            }
        } catch (Exception e) {
            logAndSave(configuration, e);
            throw e;
        }        
        return result;

    }

    public Response executeManageSearchOperation(SearchRequest searchRequest, OperationContext context) throws Exception {
        
        CustomScriptConfiguration configuration = findConfigWithGEVersion(5);
        Response result = null;

        try {
            if (configuration != null) {
                log.debug("Executing python 'SCIM Manage Search Operation' method");
                ScimType externalType = (ScimType) configuration.getExternalType();
                Map<String, SimpleCustomProperty> configurationAttributes = configuration.getConfigurationAttributes();

                result = externalType.manageSearchOperation(context, searchRequest, configurationAttributes);
                log.debug("executeManageSearchOperation result HTTP code = {}", 
                    Optional.ofNullable(result).map(res -> Integer.toString(res.getStatus())).orElse("none"));
            }
        } catch (Exception e) {
            logAndSave(configuration, e);
            throw e;
        }        
        return result;
        
    }

}
