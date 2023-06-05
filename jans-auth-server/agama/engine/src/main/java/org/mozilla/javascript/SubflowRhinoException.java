package org.mozilla.javascript;

import java.io.PrintStream;
import java.io.PrintWriter;

public class SubflowRhinoException extends Exception {

    private String rhinoStackTrace;
    
    public SubflowRhinoException(Object err) {
        
        super(err.toString());
        try {
            NativeError e = (NativeError) err;
            rhinoStackTrace = e.getStackDelegated().toString();
        } catch(ClassCastException ex) {
            rhinoStackTrace = "Stacktrace not available";
        }

    }
    
    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }
    
    @Override
    public void printStackTrace(PrintStream p) {
        p.println(getMessage());
        p.print(rhinoStackTrace);
    }
    
    @Override
    public void printStackTrace(PrintWriter w) {
        w.println(getMessage());
        w.print(rhinoStackTrace);
    }
    
}
