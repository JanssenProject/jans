package org.gluu.oxauth.client.util;

import org.gluu.oxauth.client.session.OAuthData;

/**
 * Static holder that places OAuth data in a ThreadLocal
 *
 * @author Yuriy Movchan
 * @version 0.1, 03/20/2013
 */
public class OAuthDataHolder {

    /**
     * ThreadLocal to hold the OAuth data for Threads to access
     */
    private static final ThreadLocal<OAuthData> threadLocal = new ThreadLocal<OAuthData>();


    /**
     * Retrieve the OAuth data from the ThreadLocal
     *
     * @return the OAuthData associated with this thread
     */
    public static OAuthData getOAuthData() {
        return threadLocal.get();
    }

    /**
     * Add the OAuth data to the ThreadLocal
     *
     * @param oAuthData the oAuthData to add.
     */
    public static void setOAuthData(final OAuthData oAuthData) {
        threadLocal.set(oAuthData);
    }

    /**
     * Clear the ThreadLocal
     */
    public static void clear() {
        threadLocal.set(null);
    }
}
