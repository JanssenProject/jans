package io.jans.agama.engine.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TrackingService {
    
    public void track(String flowQname, int[] lines, Exception exception) {
        //See https://github.com/JanssenProject/jans/issues/15436 and https://github.com/JanssenProject/jans/issues/15437
    }
    
    public void track(String flowQname, int line, Exception exception) {
        track(flowQname, new int[]{ line }, exception);
    }
    
}
