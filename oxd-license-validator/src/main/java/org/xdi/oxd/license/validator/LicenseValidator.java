package org.xdi.oxd.license.validator;

import com.google.common.base.Preconditions;
import net.nicholaswilliams.java.licensing.SignedLicense;
import net.nicholaswilliams.java.licensing.exception.InvalidLicenseException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.xdi.oxd.license.client.Jackson;
import org.xdi.oxd.license.client.js.LicenseMetadata;
import org.xdi.oxd.license.client.lib.ALicense;
import org.xdi.oxd.license.client.lib.ALicenseManager;
import org.xdi.oxd.license.client.lib.LicenseSerializationUtilities;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/05/2015
 */

public class LicenseValidator {

    private static final String ARGUMENTS_MESSAGE = "java org.xdi.oxd.license.validator.LicenseValidator <license> <public key> <public password> <license password>";

    public static void main(String[] args) throws IOException {
        System.out.println("Validator expects: " + ARGUMENTS_MESSAGE);
        Preconditions.checkArgument(args.length == 4, "Please specify arguments for program as following: " + ARGUMENTS_MESSAGE);

        String license = args[0];
        String publicKey = args[1];
        String publicPassword = args[2];
        String licensePassword = args[3];
        validate(publicKey, publicPassword, licensePassword, license);
    }

    public static void validate(String publicKey, String publicPassword, String licensePassword, String license) throws IOException {
        Output output = new Output();
        try {
            final SignedLicense signedLicense = LicenseSerializationUtilities.deserialize(license);

            ALicenseManager manager = new ALicenseManager(publicKey, publicPassword, signedLicense, licensePassword);

            ALicense decryptedLicense = manager.decryptAndVerifyLicense(signedLicense);// DECRYPT signed license
            manager.validateLicense(decryptedLicense);
            output.setValid(true);

            final String subject = decryptedLicense.getSubject();
            final LicenseMetadata metadata = Jackson.createJsonMapper().readValue(subject, LicenseMetadata.class);
            output.setMetadata(metadata);

        } catch (InvalidLicenseException e) {
            //System.out.println("License is invalid.");
        } catch (Exception e) {
            System.out.println("Something bad happens: " + e.getMessage());
            System.out.println(ExceptionUtils.getFullStackTrace(e));
        }
        System.out.println(Jackson.asJsonSilently(output));
    }
}
