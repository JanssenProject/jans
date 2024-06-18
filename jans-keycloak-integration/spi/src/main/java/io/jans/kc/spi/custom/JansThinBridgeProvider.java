package io.jans.kc.spi.custom;

import org.keycloak.provider.*;
import io.jans.kc.model.JansUserModel;
import io.jans.kc.model.JansUserAttributeModel;


public interface JansThinBridgeProvider extends Provider {

    JansUserAttributeModel getUserAttribute(final String kcLoginUsername, final String attributeName);
    JansUserModel getUserByUsername(final String username);
    JansUserModel getUserByEmail(final String email);
    JansUserModel getUserByInum(final String inum);
}
