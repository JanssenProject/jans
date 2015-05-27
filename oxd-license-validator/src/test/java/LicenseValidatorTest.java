import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.license.validator.LicenseValidator;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/05/2015
 */

public class LicenseValidatorTest {

    @Parameters({"publicKey", "publicPassword", "licensePassword", "license"})
    @Test
    public void test(String publicKey, String publicPassword, String licensePassword, String license) {
        LicenseValidator.validate(publicKey, publicPassword, licensePassword, license);
    }

}
