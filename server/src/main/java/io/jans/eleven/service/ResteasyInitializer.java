/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.service;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import io.jans.eleven.rest.DeleteKeyRestServiceImpl;
import io.jans.eleven.rest.GenerateKeyRestServiceImpl;
import io.jans.eleven.rest.SignRestServiceImpl;
import io.jans.eleven.rest.VerifySignatureRestServiceImpl;

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
        classes.add(GenerateKeyRestServiceImpl.class);
        classes.add(DeleteKeyRestServiceImpl.class);
        classes.add(SignRestServiceImpl.class);
        classes.add(VerifySignatureRestServiceImpl.class);

        return classes;
    }

}