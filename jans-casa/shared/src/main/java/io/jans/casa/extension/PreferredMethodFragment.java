package io.jans.casa.extension;

import org.pf4j.ExtensionPoint;

/**
 * @author jgomer
 */
public interface PreferredMethodFragment extends ExtensionPoint {

    /**
     * The URL of (potentially zul) content that will be included when 2fa is enabled in users home page (when a method
     * other than password is selected)
     * @return A string representing a relative URL.
     */
    String getUrl();

}
