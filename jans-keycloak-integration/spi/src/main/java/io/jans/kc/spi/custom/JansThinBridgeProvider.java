package io.jans.kc.spi.custom;

import org.keycloak.provider.*;

import io.jans.kc.model.JansUserAttributeModel;
import io.jans.kc.model.internal.JansPerson;


public interface JansThinBridgeProvider extends Provider {

    JansUserAttributeModel getUserAttribute(final String kcLoginUsername, final String attributeName);
    JansPerson getJansUserByUsername(final String username);
    JansPerson getJansUserByEmail(final String email);
    JansPerson getJansUserByInum(final String inum);
}
