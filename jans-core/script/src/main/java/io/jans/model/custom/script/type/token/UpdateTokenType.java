/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */

package io.jans.model.custom.script.type.token;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Movchan
 */
public interface UpdateTokenType extends BaseExternalType {

    boolean modifyIdToken(Object jsonWebResponse, Object tokenContext);

    boolean modifyRefreshToken(Object refreshToken, Object tokenContext);

    boolean modifyAccessToken(Object accessToken, Object tokenContext);

    int getRefreshTokenLifetimeInSeconds(Object tokenContext);

    int getIdTokenLifetimeInSeconds(Object context);

    int getAccessTokenLifetimeInSeconds(Object context);
}
