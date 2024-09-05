package io.jans.as.model.revoke;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.jans.as.model.common.SubId;

/**
 * @author Yuriy Z
 */
public class GlobalTokenRevocationRequest {

    @JsonProperty("sub_id")
    private SubId subId;

    public SubId getSubId() {
        return subId;
    }

    public void setSubId(SubId subId) {
        this.subId = subId;
    }
}
