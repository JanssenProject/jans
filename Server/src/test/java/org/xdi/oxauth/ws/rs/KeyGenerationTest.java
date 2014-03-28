package org.xdi.oxauth.ws.rs;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.util.security.StringEncrypter;

/**
 * @author Javier Rojas Blum Date: 05.30.2012
 */
public class KeyGenerationTest extends BaseTest {

    @Parameters({"ldapAdminPassword"})
    @Test
    public void encryptLdapPassword(final String ldapAdminPassword) throws Exception {
        showTitle("TEST: encryptLdapPassword");

        String password = StringEncrypter.defaultInstance().encrypt(ldapAdminPassword);
        System.out.println("Encrypted LDAP Password: " + password);
    }
}