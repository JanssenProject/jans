/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.web;

import com.google.inject.AbstractModule;
import org.xdi.oxd.web.ws.CommandWS;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 20/08/2015
 */

public class AppModule extends AbstractModule {

//    private static final Logger LOG = LoggerFactory.getLogger(AppModule.class);

//       private static final String ENCRYPTION_KEY = "123456789012345678901234567890";

    @Override
    protected void configure() {
        // ws
        bind(CommandWS.class);
    }
}
