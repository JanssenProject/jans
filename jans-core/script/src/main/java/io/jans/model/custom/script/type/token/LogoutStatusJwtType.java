package io.jans.model.custom.script.type.token;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Z
 */
public interface LogoutStatusJwtType extends BaseExternalType {

    boolean modifyPayload(Object jsonWebResponse, Object context);

    int getLifetimeInSeconds(Object context);
}
