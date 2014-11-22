package org.xdi.oxd.license.test;

import com.google.inject.Inject;
import junit.framework.Assert;
import net.nicholaswilliams.java.licensing.SignedLicense;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;
import org.xdi.oxd.license.client.Jackson;
import org.xdi.oxd.license.client.data.LicenseResponse;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;
import org.xdi.oxd.license.client.js.LdapLicenseId;
import org.xdi.oxd.license.client.js.LicenseMetadata;
import org.xdi.oxd.license.client.js.LicenseType;
import org.xdi.oxd.license.client.lib.ALicense;
import org.xdi.oxd.license.client.lib.ALicenseManager;
import org.xdi.oxd.license.client.lib.LicenseSerializationUtilities;
import org.xdi.oxd.licenser.server.service.LicenseCryptService;
import org.xdi.oxd.licenser.server.service.LicenseIdService;
import org.xdi.oxd.licenser.server.ws.LicenseWS;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/10/2014
 */

@Guice(modules = TestAppModule.class)
public class LicenseGeneratorTest {

    @Inject
    LicenseCryptService licenseCryptService;
    @Inject
    LicenseWS generateLicenseWS;
    @Inject
    LicenseIdService licenseIdService;

    private LdapLicenseId licenseId;
    private LdapLicenseCrypt crypt;

    @BeforeClass
    public void setUp() {
        LicenseMetadata metadata = new LicenseMetadata()
                .setLicenseType(LicenseType.PAID)
                .setMultiServer(true)
                .setThreadsCount(9);
        crypt = licenseCryptService.generate();
        licenseCryptService.save(crypt);

        licenseId = licenseIdService.generate(crypt.getDn(), metadata);
        licenseIdService.save(licenseId);
    }

    @Test
    public void generateLicense() throws IOException {
        final LicenseResponse license = generateLicenseWS.generateLicense(licenseId.getLicenseId());
        Assert.assertTrue(license != null && license.getEncodedLicense() != null);
        System.out.println("Generated license: " + license.getEncodedLicense());

        final SignedLicense signedLicense = LicenseSerializationUtilities.deserialize(license.getEncodedLicense());
        assertValidLicense(signedLicense);
        assertInValidLicense(signedLicense);

    }

    private void assertValidLicense(SignedLicense signedLicense) throws IOException {
        ALicenseManager manager = new ALicenseManager(crypt.getPublicKey(), crypt.getPublicPassword(), signedLicense, crypt.getLicensePassword());

        ALicense decryptedLicense = manager.decryptAndVerifyLicense(signedLicense);// DECRYPT signed license
        manager.validateLicense(decryptedLicense);
        System.out.println("License is valid!");

        final String subject = decryptedLicense.getSubject();
        final LicenseMetadata metadata = Jackson.createJsonMapper().readValue(subject, LicenseMetadata.class);

        System.out.println("Metadata: " + metadata);
    }

    private void assertInValidLicense(SignedLicense signedLicense) throws IOException {
        try {
            final String licensePassword = "a"; // append random char, it should fail validation!

            ALicenseManager manager = new ALicenseManager(crypt.getPublicKey(), crypt.getPublicPassword(), signedLicense, licensePassword);

            ALicense decryptedLicense = manager.decryptAndVerifyLicense(signedLicense);// DECRYPT signed license
            manager.validateLicense(decryptedLicense);
            System.out.println("License is valid!");

            final String subject = decryptedLicense.getSubject();
            final LicenseMetadata metadata = Jackson.createJsonMapper().readValue(subject, LicenseMetadata.class);

            System.out.println("Metadata: " + metadata);
        } catch (Exception e) {
            // we are lucky and it fails
            return;
        }
        throw new RuntimeException("Validation passed even with WRONG license password. Something went wrong :(");
    }
}
