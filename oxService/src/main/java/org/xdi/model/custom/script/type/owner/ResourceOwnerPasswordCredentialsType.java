package org.xdi.model.custom.script.type.owner;

import org.xdi.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface ResourceOwnerPasswordCredentialsType extends BaseExternalType {

    boolean authenticate(Object context);
}
