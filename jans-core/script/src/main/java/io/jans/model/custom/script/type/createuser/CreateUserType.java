package io.jans.model.custom.script.type.createuser;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Z
 */
public interface CreateUserType extends BaseExternalType {

    String getCreateUserPage(Object context);

    boolean prepare(Object context);

    boolean createUser(Object context);

    String buildPostAuthorizeUrl(Object context);
}
