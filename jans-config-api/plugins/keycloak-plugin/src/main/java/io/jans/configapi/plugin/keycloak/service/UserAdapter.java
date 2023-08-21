package io.jans.configapi.plugin.keycloak.service;

import io.jans.configapi.plugin.mgt.model.user.CustomUser;
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;

public class UserAdapter extends AbstractUserAdapter {

    private final CustomUser user;

    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, CustomUser user) {
        super(session, realm, model);
        this.storageId = new StorageId(storageProviderModel.getId(), user.getUserId());
        this.user = user;
    }

    @Override
    public String getUsername() {
        return user.getGivenName();
    }

    @Override
    public String getFirstName() {
        return user.getAttribute("firstName");
    }

    @Override
    public String getLastName() {
        return user.getAttribute("lastName");
    }

    @Override
    public String getEmail() {
        return user.getAttribute("email");
    }

    @Override
    public SubjectCredentialManager credentialManager() {
        return new LegacyUserCredentialManager(session, realm, this);
    }

    @Override
    public boolean isEnabled() {
        boolean enabled = false;
        if(StringUtils.isNotBlank(user.getJansStatus())){
            enabled =  user.getJansStatus().equalsIgnoreCase("active")?true:false;
        }
        return enabled;
    }

    @Override
    public Long getCreatedTimestamp() {
        Long created = null;
        if(user.getCreatedAt()!=null) {
            created = user.getCreatedAt().getTime();
        }
        return created;
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
 
}
