package org.xdi.oxd.license.client;

import com.google.common.base.Strings;
import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.license.client.data.License;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/09/2014
 */

public class LicenseClientTest {

    @Parameters({"licenseServerEndpoint"})
    @Test
    public void generateLicense(String licenseServerEndpoint) {
        final GenerateWS generateWS = LicenseClient.generateWs(licenseServerEndpoint);

        final License generatedLicense = generateWS.generate();

        Assert.assertTrue(generatedLicense != null && !Strings.isNullOrEmpty(generatedLicense.getLicense()));
        System.out.println(generatedLicense.getLicense());
    }
}
