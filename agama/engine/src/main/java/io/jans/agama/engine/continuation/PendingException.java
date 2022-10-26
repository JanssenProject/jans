package io.jans.agama.engine.continuation;

import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.NativeContinuation;

public class PendingException extends ContinuationPending {

    private boolean allowCallbackResume;
    
    public PendingException(NativeContinuation continuation) {
        super(continuation);
    }
    
    @Override
    public NativeContinuation getContinuation() {
        return (NativeContinuation) super.getContinuation();
    }

    public boolean isAllowCallbackResume() {
        return allowCallbackResume;
    }

    public void setAllowCallbackResume(boolean allowCallbackResume) {
        this.allowCallbackResume = allowCallbackResume;
    }
    
}
