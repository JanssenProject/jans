/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.jwk.ws.rs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.wordnik.swagger.annotations.Api;

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
@Api(value = "/", description = "JWK Endpoint provides list of JWK used by server. A JSON Web Key (JWK) is a JSON data structure that represents a set of public keys as a JSON object [RFC4627].")
public interface JwkRestWebService {

    /**
     * The JWK endpoint.
     *
     * @param securityContext An injectable interface that provides access to security
     *                        related information.
     * @return The JSON Web Key data structure JWK. A JWK consists of a JWK Container Object, which is a JSON object
     *         that contains an array of JWK Key Objects as a member.
     */
    @GET
    @Path("/jwks")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestJwk(@Context SecurityContext securityContext);
}