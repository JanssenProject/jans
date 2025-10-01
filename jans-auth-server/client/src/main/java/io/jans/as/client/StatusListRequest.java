package io.jans.as.client;

import io.jans.as.model.config.Constants;

/**
 * @author Yuriy Z
 */
public class StatusListRequest extends BaseRequest {

    public StatusListRequest() {
        setContentType(Constants.CONTENT_TYPE_STATUSLIST_JSON);
    }

    @Override
    public String getQueryString() {
        return null;
    }
}
