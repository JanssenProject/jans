package org.gluu.model.custom.script.type.owner;

import org.gluu.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface ResourceOwnerPasswordCredentialsType extends BaseExternalType {

    boolean authenticate(Object context);
}
