package org.gluu.oxd.rs.protect.resteasy;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/04/2016
 */

public interface PatProvider {

    String getPatToken();

    void clearPat();
}
