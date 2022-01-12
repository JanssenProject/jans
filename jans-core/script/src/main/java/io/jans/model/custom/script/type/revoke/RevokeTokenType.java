package io.jans.model.custom.script.type.revoke;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface RevokeTokenType extends BaseExternalType {

    boolean revoke(Object context);
}
