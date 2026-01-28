package io.jans.casa.certauthn;

import io.jans.agama.engine.service.LabelsService;
import io.jans.service.cdi.util.CdiUtil;

public final class LabelUtils {

    private static LabelsService lbls = CdiUtil.bean(LabelsService.class);
    
    public static String get(String key, Object... args) {
        return lbls.get(key, args);
    }
    
    public static String get(String key) {
        return get(key, new Object[0]);
    }
    
    public static String getWithPrefix(String prefix, String key) {
        return get(prefix + key, new Object[0]);
    }
    
    private LabelUtils();
    
}
