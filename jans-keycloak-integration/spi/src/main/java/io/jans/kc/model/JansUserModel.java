package io.jans.kc.model;

import io.jans.kc.model.internal.JansPerson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;

public class JansUserModel implements UserModel {
    
    private static final String INUM_ATTR_NAME = "inum";
    private static final String UID_ATTR_NAME = "uid";
    private static final String JANS_CREATION_TIMESTAMP_ATTR_NAME = "jansCreationTimestamp";
    private static final String JANS_STATUS_ATTR_NAME = "jansStatus";
    private static final String GIVEN_NAME_ATTR_NAME = "givenName";
    private static final String MAIL_ATTR_NAME = "mail";
    private static final String EMAIL_VERIFIED_ATTR_NAME = "emailVerified";
    private static final String USER_READ_ONLY_EXCEPTION_MSG = "User is read-only for this update";

    private final JansPerson jansPerson;
    private final StorageId storageId;

    public JansUserModel(ComponentModel storageProviderModel, JansPerson jansPerson) {

        this.jansPerson = jansPerson;
        String userId = jansPerson.customAttributeValue(INUM_ATTR_NAME);
        this.storageId = new StorageId(storageProviderModel.getId(),userId);
    }

    @Override
    public String getId() {
        
        return storageId.getId();
    }

    @Override
    public String getUsername() {

        return jansPerson.customAttributeValue(UID_ATTR_NAME);
    }

    @Override
    public void setUsername(String username) {

        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public Long getCreatedTimestamp() {

        try {
            final String createdStr = jansPerson.customAttributeValue(JANS_CREATION_TIMESTAMP_ATTR_NAME);
            if(createdStr == null) {
                return null;
            }
            return Long.parseLong(createdStr);
        }catch(NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {

        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public boolean isEnabled()  {
        
        final String enabledStr = jansPerson.customAttributeValue(JANS_STATUS_ATTR_NAME);
        if(enabledStr == null) {
            return false;
        }
        return "active".equals(enabledStr);
    }

    @Override
    public void setEnabled(boolean enabled) {
        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public void setSingleAttribute(String name, String value)  {
        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public void setAttribute(String name, List<String> value) {

        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public void removeAttribute(String name) {
        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }


    @Override
    public String getFirstAttribute(String name) {

        if(USERNAME.equals(name)) {
            return getUsername();
        }else if(FIRST_NAME.equals(name)) {
            return getFirstName();
        }else if(EMAIL.equals(name)) {
            return getEmail();
        }else {
            return jansPerson.customAttributeValue(name);
        }
    }

    @Override
    public Stream<String> getAttributeStream(final String name) {

        List<String> ret = new ArrayList<>();

        if(USERNAME.equals(name)) {
            ret.add(getUsername());
        }else if(FIRST_NAME.equals(name)) {
            ret.add(getFirstName());
        }else if(EMAIL.equals(name)) {
            ret.add(getEmail());
        }else {
            return jansPerson.customAttributeValues(name).stream();
        }
        return ret.stream();
    }

    @Override
    public Map<String,List<String>> getAttributes() {

        Map<String,List<String>> ret = new HashMap<>();
        for(String attrName : jansPerson.customAttributeNames()) {
            ret.put(attrName,jansPerson.customAttributeValues(attrName));
        }
        return ret;
    }

    @Override
    public Stream<String> getRequiredActionsStream() {

        return new ArrayList<String>().stream();
    }

    @Override
    public void addRequiredAction(String action) {
        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public void removeRequiredAction(String action) {
        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public String getFirstName() {

        return jansPerson.customAttributeValue(GIVEN_NAME_ATTR_NAME);
    }

    @Override
    public void setFirstName(String firstName) {

        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public String getLastName() {

        return null;
    }

    @Override
    public void setLastName(String lastName) {

        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public String getEmail() {

        return jansPerson.customAttributeValue(MAIL_ATTR_NAME);
    }

    @Override
    public void setEmail(final String email) {

        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public boolean isEmailVerified() {
        
        try {
            final String attr = jansPerson.customAttributeValue(EMAIL_VERIFIED_ATTR_NAME);
            if(attr == null) {
                return false;
            }
            return Boolean.parseBoolean(attr);
        }catch(NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void setEmailVerified(boolean verified) {
        
        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {

        return new ArrayList<GroupModel>().stream();
    }

    @Override
    public long getGroupsCount() {

        return 0;
    }

    @Override
    public long getGroupsCountByNameContaining(String search) {

        return 0;
    }

    @Override
    public void joinGroup(GroupModel group) {
        
        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public void leaveGroup(GroupModel group) {

        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public boolean isMemberOf(GroupModel group) {

        return false;
    }

    @Override
    public String getFederationLink() {

        return null;
    }

    @Override
    public void setFederationLink(String link) {

        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public String getServiceAccountClientLink() {

        return null;
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {

        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public SubjectCredentialManager credentialManager() {

        return null;
    }

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {

        return new ArrayList<RoleModel>().stream();
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {

        return new ArrayList<RoleModel>().stream();
    }

    @Override
    public boolean hasRole(RoleModel role) {

        return false;
    }

    @Override
    public void grantRole(RoleModel role) {

        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {

        return new ArrayList<RoleModel>().stream();
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {

        throw new ReadOnlyException(USER_READ_ONLY_EXCEPTION_MSG);
    }

}
