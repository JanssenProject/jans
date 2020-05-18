package org.gluu.model.custom.script.type.postauthn;

import org.gluu.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface PostAuthnType extends BaseExternalType {

    boolean forceReAuthentication(Object context);

    boolean forceAuthorization(Object context);
}
