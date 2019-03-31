package org.gluu.oxd.common.params;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 28/04/2017
 */

public interface HasProtectionAccessTokenParams extends HasOxdIdParams {
    String getProtectionAccessToken();

    void setProtectionAccessToken(String protectionAccessToken);
}
