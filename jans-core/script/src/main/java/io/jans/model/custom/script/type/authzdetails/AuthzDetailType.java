package io.jans.model.custom.script.type.authzdetails;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * Authorization Details Script
 *
 * @author Yuriy Z
 */
public interface AuthzDetailType extends BaseExternalType {

    boolean validateDetail(Object context);

    String getUiRepresentation(Object context);
}

