/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.license.LicenseService;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/11/2014
 */

public class GuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(LicenseService.class).in(Singleton.class);
    }

    @Provides
    public Configuration provideConfiguration() {
        return Configuration.getInstance();
    }

}
