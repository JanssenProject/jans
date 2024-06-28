package io.jans.as.client;

import io.jans.as.model.config.Constants;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.session.EndSessionErrorResponseType;
import io.jans.model.tokenstatus.StatusList;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

import java.io.IOException;

/**
 * @author Yuriy Z
 */
public class StatusListResponse extends BaseResponseWithErrors<EndSessionErrorResponseType> {

    private String lst;
    private int bits;
    private Jwt jwt;

    public StatusListResponse() {
    }

    public StatusListResponse(Response clientResponse) {
        super(clientResponse);
        injectData(clientResponse);
    }

    public StatusList getStatusList() throws IOException {
        return StatusList.fromEncoded(lst, bits);
    }

    @Override
    public EndSessionErrorResponseType fromString(String params) {
        return EndSessionErrorResponseType.fromString(params);
    }

    public void injectData(Response clientResponse) {
        injectErrorIfExistSilently(entity);
        if (getErrorType() != null) {
            return;
        }

        if (clientResponse.getStatus() != 200) {
            return;
        }

        final String contentType = clientResponse.getHeaderString("Content-Type");
        if (Constants.CONTENT_TYPE_STATUSLIST_JWT.equalsIgnoreCase(contentType)) {
            jwt = Jwt.parseSilently(entity);
            if (jwt != null) {
                final JSONObject statusList = jwt.getClaims().getClaimAsJSON("status_list");
                lst = statusList.getString("lst");
                bits = statusList.getInt("bits");
            }
        } else if (Constants.CONTENT_TYPE_STATUSLIST_JSON.equalsIgnoreCase(contentType)) {
            final JSONObject json = new JSONObject(entity);
            final JSONObject statusList = json.getJSONObject("status_list");
            lst = statusList.getString("lst");
            bits = statusList.getInt("bits");
        } else {
            throw new UnsupportedOperationException("Unable to recognize content-type: " + contentType);
        }
    }

    public String getLst() {
        return lst;
    }

    public void setLst(String lst) {
        this.lst = lst;
    }

    public int getBits() {
        return bits;
    }

    public void setBits(int bits) {
        this.bits = bits;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    @Override
    public String toString() {
        return "StatusListResponse{" +
                "lst='" + lst + '\'' +
                ", bits=" + bits +
                ", jwt=" + jwt +
                "} " + super.toString();
    }
}
