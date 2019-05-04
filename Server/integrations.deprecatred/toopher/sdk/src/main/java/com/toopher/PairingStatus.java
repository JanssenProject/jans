package com.toopher;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provides information about the status of a pairing request
 * 
 */
public class PairingStatus {
    /**
     * The unique id for the pairing request
     */
    public String id;

    /**
     * The unique id for the user associated with the pairing request
     */
    public String userId;

    /**
     * The descriptive name for the user associated with the pairing request
     */
    public String userName;

    /**
     * Indicates if the pairing has been enabled by the user
     */
    public boolean enabled;

    @Override
    public String toString() {
        return String.format("[PairingStatus: id=%s; userId=%s; userName=%s, enabled=%b]", id,
                             userId, userName, enabled);
    }

    static PairingStatus fromJSON(JSONObject json) throws JSONException {
        PairingStatus ps = new PairingStatus();
        ps.id = json.getString("id");

        JSONObject user = json.getJSONObject("user");
        ps.userId = user.getString("id");
        ps.userName = user.getString("name");

        ps.enabled = json.getBoolean("enabled");

        return ps;
    }
}
