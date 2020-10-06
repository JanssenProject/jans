/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package io.jans.notify.resource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * Integration with Resteasy
 * 
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
@ApplicationPath("/restv1")
public class ResteasyInitializer extends Application {	
}