package org.xdi.oxd.license.client.lib;

import net.nicholaswilliams.java.licensing.exception.InvalidLicenseException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/09/2014
 */

public interface ILicenseValidator {
    public void validateLicense(ALicense license) throws InvalidLicenseException;
}
