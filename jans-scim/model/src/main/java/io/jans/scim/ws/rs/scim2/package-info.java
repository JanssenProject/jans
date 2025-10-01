/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

/*
 * Interfaces found here are common "contracts" so if a service method is added or changes signature, both projects
 * (scim-rest and scim-client) will consistently "see" the same contract.
 * Important: do not use @Inject in these classes (the client is not a weld project)
 */
/**
 * Interfaces and annotations shared by both the server side code and the Java client..
 * Client code uses the Resteasy Proxy Framework and thus we are sharing the same interface for client and server.
 */
package io.jans.scim.ws.rs.scim2;