package org.mozilla.javascript;

public class NativeErrorUtil {

    public static String stackTraceOf(Object err) {
        
        try {
            return NativeError.class.cast(err).getStackDelegated().toString();
        } catch (Exception ex) {
            return "Stacktrace not available. " + ex.getMessage();
        }

    }

    
}
