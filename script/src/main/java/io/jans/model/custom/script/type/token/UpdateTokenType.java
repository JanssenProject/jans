package io.jans.model.custom.script.type.token;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Movchan
 */
public interface UpdateTokenType extends BaseExternalType {

    boolean modifyIdToken(Object jsonWebResponse, Object tokenContext);
}
