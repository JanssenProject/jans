package org.xdi.oxauth.model.discovery;

/**
 * @author Javier Rojas Blum Date: 01.28.2013
 */
public class WebFingerLink {

    private String rel;
    private String href;

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}