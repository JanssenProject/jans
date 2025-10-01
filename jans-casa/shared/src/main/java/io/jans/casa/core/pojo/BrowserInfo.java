package io.jans.casa.core.pojo;

/**
 * A POJO holding basic information about a user's browser.
 */
public class BrowserInfo {

    private String name;
    private int mainVersion;
    private boolean mobile;

    /**
     * Returns the name of web browser (e.g. Internet Explorer).
     * @return A String
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the main (major) version (e.g. 11).
     * @return An int value
     */
    public int getMainVersion() {
        return mainVersion;
    }

    /**
     * Whether this is a mobile browser or not.
     * @return A boolean value
     */
    public boolean isMobile() {
        return mobile;
    }

    /**
     * Sets the name of the browser in this <code>BrowserInfo</code> instance.
     * @param name A string value
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the (main) version of the browser in this <code>BrowserInfo</code> instance.
     * @param mainVersion An int value
     */
    public void setMainVersion(int mainVersion) {
        this.mainVersion = mainVersion;
    }

    /**
     * Sets whether the browser is mobile or not in this <code>BrowserInfo</code> instance.
     * @param mobile A boolean value
     */
    public void setMobile(boolean mobile) {
        this.mobile = mobile;
    }

}
