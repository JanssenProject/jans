package com.toopher;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provide information about the status of an authentication request
 * 
 */
public class AuthenticationStatus {
    /**
     * The unique id for the authentication request
     */
    public String id;

    /**
     * Indicates if the request is still pending
     */
    public boolean pending;

    /**
     * Indicates if the request was granted
     */
    public boolean granted;

    /**
     * Indicates if the request was automated
     */
    public boolean automated;

    /**
     * Indicates if the request was cancelled
     */
    public boolean cancelled;

    /**
     * Indicates the reason (if any) for the request's outcome
     */
    public String reason;

    /**
     * The unique id for the terminal associated with the request
     */
    public String terminalId;

    /**
     * The descriptive name for the terminal associated with the request
     */
    public String terminalName;

    @Override
    public String toString() {
        return String.format("[AuthenticationStatus: id=%s; pending=%b; granted=%b; automated=%b; cancelled=%d; reason=%s; terminalId=%s; terminalName=%s]",
                             id, pending, granted, automated, cancelled, reason, terminalId, terminalName);
    }

    static AuthenticationStatus fromJSON(JSONObject json) throws JSONException {
        AuthenticationStatus as = new AuthenticationStatus();
        as.id = json.getString("id");
        as.pending = json.getBoolean("pending");
        as.granted = json.getBoolean("granted");
        as.automated = json.getBoolean("automated");
        as.cancelled = json.getBoolean("cancelled");
        as.reason = json.getString("reason");

        JSONObject terminal = json.getJSONObject("terminal");
        as.terminalId = terminal.getString("id");
        as.terminalName = terminal.getString("name");

        return as;
    }
}
