package io.jans.agama.engine.continuation;

import org.mozilla.javascript.NativeContinuation;

public class PendingRedirectException extends PendingException {
    
    public PendingRedirectException(NativeContinuation continuation) {
        super(continuation);
    }

    private String location;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

}
