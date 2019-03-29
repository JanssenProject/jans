/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

/**
 * @author Javier Rojas Blum Date: 03.30.2012
 */
public enum AuthorizationMethod {

    /**
     * When sending the access token in the "Authorization" request header
     * field defined by HTTP/1.1, Part 7 [I-D.ietf-httpbis-p7-auth], the
     * client uses the "Bearer" authentication scheme to transmit the access
     * token.
     */
    AUTHORIZATION_REQUEST_HEADER_FIELD,
    /**
     * When sending the access token in the HTTP request entity-body, the
     * client adds the access token to the request body using the
     * "access_token" parameter.  The client MUST NOT use this method unless
     * all of the following conditions are met:
     * <p>
     * - The HTTP request entity-header includes the "Content-Type" header
     * field set to "application/x-www-form-urlencoded".
     * <p>
     * - The entity-body follows the encoding requirements of the
     * "application/x-www-form-urlencoded" content-type as defined by
     * HTML 4.01 [W3C.REC-html401-19991224].
     * <p>
     * - The HTTP request entity-body is single-part.
     * <p>
     * - The content to be encoded in the entity-body MUST consist entirely
     * of ASCII [USASCII] characters.
     * <p>
     * - The HTTP request method is one for which the request body has
     * defined semantics.  In particular, this means that the "GET"
     * method MUST NOT be used.
     * <p>
     * The entity-body MAY include other request-specific parameters, in
     * which case, the "access_token" parameter MUST be properly separated
     * from the request-specific parameters using "&amp;" character(s) (ASCII
     * code 38).
     */
    FORM_ENCODED_BODY_PARAMETER,
    /**
     * When sending the access token in the HTTP request URI, the client
     * adds the access token to the request URI query component as defined
     * by Uniform Resource Identifier (URI) [RFC3986] using the
     * "access_token" parameter.
     * <p>
     * The HTTP request URI query can include other request-specific
     * parameters, in which case, the "access_token" parameter MUST be
     * properly separated from the request-specific parameters using "&amp;"
     * character(s) (ASCII code 38).
     * <p>
     * Because of the security weaknesses associated with the URI method
     * (see Section 5), including the high likelihood that the URL
     * containing the access token will be logged, it SHOULD NOT be used
     * unless it is impossible to transport the access token in the
     * "Authorization" request header field or the HTTP request entity-body.
     * Resource servers MAY support this method.
     */
    URL_QUERY_PARAMETER;
}