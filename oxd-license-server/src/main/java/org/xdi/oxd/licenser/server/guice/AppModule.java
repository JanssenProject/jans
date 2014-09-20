package org.xdi.oxd.licenser.server.guice;

import com.google.inject.AbstractModule;
import org.xdi.oxd.licenser.server.KeyGenerator;
import org.xdi.oxd.licenser.server.LicenseGenerator;
import org.xdi.oxd.licenser.server.ws.GenerateLicenseWS;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/09/2014
 */

public class AppModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(KeyGenerator.class);
        bind(LicenseGenerator.class);

        // ws
        bind(GenerateLicenseWS.class);
    }
}