/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.app;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import io.jans.fido2.ws.rs.controller.AssertionController;
import io.jans.fido2.ws.rs.controller.AttestationController;
import io.jans.fido2.ws.rs.controller.ConfigurationController;

/**
 * Integration with Resteasy
 * 
 * @author Yuriy Movchan
 * @version 0.1, 03/21/2017
 */
@ApplicationPath("/restv1")
public class ResteasyInitializer extends Application {	

	@Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(ConfigurationController.class);
        classes.add(AssertionController.class);
        classes.add(AttestationController.class);

        return classes;
    }

}