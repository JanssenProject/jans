package io.jans.idp.keycloak.service;

import io.jans.idp.keycloak.util.JansDataUtil;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.model.scim2.util.DateUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.LegacyUserCredentialManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserAdapter extends AbstractUserAdapter {
    private static Logger logger = LoggerFactory.getLogger(UserAdapter.class);
    private final UserResource user;

    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, UserResource user) {

        super(session, realm, model);
        logger.debug(
                " UserAdapter() - model:{}, user:{}, storageProviderModel:{}, storageProviderModel.getId():{}, user.getId():{}",
                model, user, storageProviderModel, storageProviderModel.getId(), user.getId());
        this.storageId = new StorageId(storageProviderModel.getId(), user.getId());
        this.user = user;

        logger.debug("UserAdapter() - All User Resource field():{}", printUserResourceField());
    }

    @Override
    public String getUsername() {
        return user.getUserName();
    }

    @Override
    public String getFirstName() {
        return user.getDisplayName();
    }

    @Override
    public String getLastName() {
        return user.getNickName();
    }

    @Override
    public String getEmail() {
        return ((user.getEmails() != null && user.getEmails().get(0) != null) ? user.getEmails().get(0).getValue()
                : null);
    }

    @Override
    public SubjectCredentialManager credentialManager() {
        return new LegacyUserCredentialManager(session, realm, this);
    }

    public Map<String, Object> getCustomAttributes() {
        printUserCustomAttributes();
        return user.getCustomAttributes();
    }

    @Override
    public boolean isEnabled() {
        boolean enabled = false;
        if (user != null) {
            enabled = user.getActive();
        }
        return enabled;
    }

    @Override
    public Long getCreatedTimestamp() {
        Long createdDate = null;
        if (user.getMeta().getCreated() != null) {
            String created = user.getMeta().getCreated();
            if (created != null && StringUtils.isNotBlank(created)) {
                createdDate = DateUtil.ISOToMillis(created);
            }
        }
        return createdDate;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
        attributes.add(UserModel.USERNAME, getUsername());
        attributes.add(UserModel.EMAIL, getEmail());
        attributes.add(UserModel.FIRST_NAME, getFirstName());
        attributes.add(UserModel.LAST_NAME, getLastName());
        return attributes;
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        if (name.equals(UserModel.USERNAME)) {
            return Stream.of(getUsername());
        }
        return Stream.empty();
    }

    @Override
    protected Set<RoleModel> getRoleMappingsInternal() {
        return Set.of();
    }

    private void printUserCustomAttributes() {
        logger.info(" UserAdapter::printUserCustomAttributes() - user:{}", user);
        if (user == null || user.getCustomAttributes() == null || user.getCustomAttributes().isEmpty()) {
            return;
        }

        user.getCustomAttributes().keySet().stream()
                .forEach(key -> logger.info("key:{} , value:{}", key, user.getCustomAttributes().get(key)));

    }

    private Map<String, String> printUserResourceField() {
        logger.info(" UserAdapter::printUserResourceField() - user:{}", user);
        Map<String, String> propertyTypeMap = null;
        if (user == null) {
            return propertyTypeMap;
        }

        propertyTypeMap = JansDataUtil.getFieldTypeMap(user.getClass());
        logger.info("UserAdapter::printUserResourceField() - all fields of user:{}", propertyTypeMap);
        return propertyTypeMap;
    }

}
