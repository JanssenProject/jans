package io.jans.casa.service;

import java.io.File;

/**
 * @author jgomer
 */
public interface IBrandingManager {

    String CUSTOM_FILEPATH = System.getProperty("server.base") + File.separator + "static";

    String getLogoUrl();
    String getFaviconUrl();
    String getPrefix();
    String getExtraCss();
    void setLogoContent(byte[] blob) throws Exception;
    void setFaviconContent(byte[] blob) throws Exception;
    void useExtraCss(String css) throws Exception;
    void useExternalAssets() throws Exception;
    void factoryReset() throws Exception;

}
