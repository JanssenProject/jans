package io.jans.casa.plugins.sampleauthn;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class SampleCredentialPlugin extends Plugin {

    public SampleCredentialPlugin(PluginWrapper wrapper) {
        super(wrapper);
        //Add code here for initialization-related tasks. Good candidates are:
        // - Initialization of singleton variables
        // - Obtain application-scoped bean references
        // - One-time computations 
    }

    @Override
    public void start() {
        //Plugins have a lifecycle (see https://pf4j.org/doc/plugins.html)
        //In Casa we follow a simpler approach: when a plugin archive is added,
        //the plugin is instantiated and then started. If these operations succeeded, 
        //the plugin can then be removed - in this case it will be stopped and then deleted
        
        //Use this method for additional initialization tasks not covered in the constructor
    }

    @Override
    public void delete() {
        //Use this method for clean-up related duties
    }
    
}
