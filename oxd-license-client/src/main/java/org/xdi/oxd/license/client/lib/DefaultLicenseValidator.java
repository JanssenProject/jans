package org.xdi.oxd.license.client.lib;

import net.nicholaswilliams.java.licensing.exception.ExpiredLicenseException;
import net.nicholaswilliams.java.licensing.exception.InvalidLicenseException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/09/2014
 */

public class DefaultLicenseValidator implements ILicenseValidator {

    @Override
    public void validateLicense(ALicense license) throws InvalidLicenseException {
        long time = Calendar.getInstance().getTimeInMillis();
        if (license.getGoodAfterDate() > time)
            throw new InvalidLicenseException("The " + this.getLicenseDescription(license) + " does not take effect until " + this.getFormattedDate(license.getGoodAfterDate()) + ".");
        if (license.getGoodBeforeDate() < time)
            throw new ExpiredLicenseException("The " + this.getLicenseDescription(license) + " expired on " + this.getFormattedDate(license.getGoodAfterDate()) + ".");
    }

    public String getLicenseDescription(ALicense license) {
        return license.getSubject() + " license for " + license.getHolder();
    }

    public String getFormattedDate(long time) {
        return new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z (Z)").format(new Date(time));
    }
}
