package io.jans.model.custom.script.type.postauthn;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface PostAuthnType extends BaseExternalType {

    boolean forceReAuthentication(Object context);

    boolean forceAuthorization(Object context);
}
