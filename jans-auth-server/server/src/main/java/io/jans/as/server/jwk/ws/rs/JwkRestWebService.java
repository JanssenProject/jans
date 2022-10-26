/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.jwk.ws.rs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * <p>
 * Provides interface for JWK REST web services
 * </p>
 * <p>
 * A JSON Web Key (JWK) is a JSON data structure that represents a set of public keys as a JSON object [RFC4627].
 * The JWK format is used to represent bare keys. JSON Web Keys are referenced in JSON Web Signatures (JWSs)
 * using the jku (JSON Key URL) header parameter.
 * </p>
 * <p>
 * It is sometimes useful to be able to reference public key representations, for instance, in order to verify the
 * signature on content signed with the corresponding private key. The JSON Web Key (JWK) data structure provides a
 * convenient JSON representation for sets of public keys utilizing either the Elliptic Curve or RSA families of
 * algorithms.
 * </p>
 *
 * @author Javier Rojas Blum Date: 11.15.2011
 */
public interface JwkRestWebService {

    /**
     * The JWK endpoint.
     *
     * @param securityContext An injectable interface that provides access to security
     *                        related information.
     * @return The JSON Web Key data structure JWK. A JWK consists of a JWK Container Object, which is a JSON object
     * that contains an array of JWK Key Objects as a member.
     */
    @GET
    @Path("/jwks")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestJwk(@Context SecurityContext securityContext);
}