package io.jans.agama;

import io.jans.agama.engine.service.ActionService;
import io.jans.service.cdi.util.CdiUtil;

public class Utils {
        
    public static ClassLoader agamaClassLoader() {
        return CdiUtil.bean(ActionService.class).getClassLoader();
    }
    
    private Utils() { }
    
}
