package org.xdi.oxd.common.response;

import com.google.common.collect.Maps;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2015
 */

public class GetUserInfoResponse implements IOpResponse {

    @JsonProperty("claims")
    private Map<String, List<String>> claims = Maps.newHashMap();

    public GetUserInfoResponse() {
    }

    public GetUserInfoResponse(Map<String, List<String>> claims) {
        this.claims = claims;
    }

    public Map<String, List<String>> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, List<String>> claims) {
        this.claims = claims;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GetUserInfoResponse");
        sb.append("{claims=").append(claims);
        sb.append('}');
        return sb.toString();
    }
}
