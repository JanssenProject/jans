/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.custom.script.type.client;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.type.BaseExternalType;

import java.util.Map;

/**
 * Base interface for external custom client registration python script
 *
 * @author Yuriy Movchan Date: 11/11/2014
 */
public interface ClientRegistrationType extends BaseExternalType {

    // context - io.jans.as.server.service.external.context.DynamicClientRegistrationContext
    boolean createClient(Object context);

    // context - io.jans.as.server.service.external.context.DynamicClientRegistrationContext
    boolean updateClient(Object context);

    String getSoftwareStatementHmacSecret(Object context);

    String getSoftwareStatementJwks(Object context);

    String getDcrHmacSecret(Object context);

    String getDcrJwks(Object context);

    // context - io.jans.as.server.service.external.context.DynamicClientRegistrationContext
    // cert - java.security.cert.X509Certificate
    boolean isCertValidForClient(Object cert, Object context);

    boolean modifyPutResponse(Object responseAsJsonObject, Object executionContext);

    boolean modifyReadResponse(Object responseAsJsonObject, Object executionContext);

    boolean modifyPostResponse(Object responseAsJsonObject, Object executionContext);
}
