package org.xdi.oxd.license.test;
/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.ejbca.core.protocol.ws.client.gen.CertificateResponse;
import org.ejbca.core.protocol.ws.client.gen.UserDataVOWS;
import org.ejbca.core.protocol.ws.common.CertificateHelper;
import org.testng.annotations.*;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;
import org.xdi.oxd.license.client.js.LdapLicenseId;
import org.xdi.oxd.license.client.js.LicenseMetadata;
import org.xdi.oxd.license.client.js.LicenseType;
import org.xdi.oxd.licenser.server.service.EjbCaService;
import org.xdi.oxd.licenser.server.service.LicenseCryptService;
import org.xdi.oxd.licenser.server.service.LicenseIdService;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author yuriyz on 12/14/2014.
 */
@Guice(modules = org.xdi.oxd.license.test.TestAppModule.class)
public class EjbcaServiceTest {

    @Inject
    LicenseCryptService licenseCryptService;
    @Inject
    LicenseIdService licenseIdService;
    @Inject
    EjbCaService ejbCaService;
    private LdapLicenseId licenseId;
    private LdapLicenseCrypt crypt;

    @Parameters({"gluu.ejbca.storePath"})
    @BeforeClass
    public void setUp(String storePath) {
        System.setProperty("gluu.ejbca.storePath", storePath);

        LicenseMetadata metadata = new LicenseMetadata()
                .setLicenseType(LicenseType.PAID)
                .setMultiServer(true)
                .setThreadsCount(9);
        crypt = licenseCryptService.generate();
        licenseCryptService.save(crypt);

        licenseId = licenseIdService.generate(crypt.getDn(), metadata);
        licenseIdService.save(licenseId);
    }

    @AfterClass
    public void tearDown() {
        licenseIdService.remove(licenseId);
        licenseCryptService.remove(crypt);
    }

    @Test
    public void createUserAndSignCsr() {
        UserDataVOWS user = ejbCaService.createUser(licenseId);

        CertificateResponse certificateResponse = ejbCaService.signCsr(licenseId.getLicenseId(), "secret", testCsr(), CertificateHelper.RESPONSETYPE_CERTIFICATE);
        System.out.println("==> Signed certificate.");
        System.out.println(new String(certificateResponse.getData()));

        // remove user
        ejbCaService.removeUser(licenseId);
    }

    private static String testCsr() {
        final InputStream inputStream = org.xdi.oxd.license.test.EjbcaManualTest.class.getResourceAsStream("csr.txt");
        try {
            return IOUtils.toString(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
