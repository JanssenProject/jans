package io.jans.agama.engine.script;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import jakarta.enterprise.inject.spi.CDI;

import io.jans.agama.engine.continuation.PendingRedirectException;
import io.jans.agama.engine.continuation.PendingRenderException;
import io.jans.agama.engine.misc.PrimitiveUtils;
import io.jans.agama.engine.service.ActionService;
import io.jans.agama.engine.service.FlowService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.Pair;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeContinuation;
import org.mozilla.javascript.NativeJavaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptUtils.class);
    
    // NOTE: do not alter this method's signature so that it returns void. The returned 
    // value is simulated when the continuation is resumed: see 3rd parameter in call
    // to resumeContinuation (FlowService)
    public static Pair<Boolean, String> pauseForRender(String page, boolean allowCallbackResume, Object data)
            throws PendingRenderException {
        
        Context cx = Context.enter();
        try {
            PendingRenderException pending = new PendingRenderException(
                    (NativeContinuation) cx.captureContinuation().getContinuation());
            pending.setTemplatePath(page);
            pending.setDataModel((Map<String, Object>) data);
            pending.setAllowCallbackResume(allowCallbackResume);
            LOG.debug("Pausing flow");
            throw pending;
        } finally {
            Context.exit();
        }

    }

    // NOTE: do not alter this method's signature so that it returns void. The returned 
    // value is simulated when the continuation is resumed: see 3rd parameter in call
    // to resumeContinuation (FlowService)    
    public static Pair<Boolean, String> pauseForExternalRedirect(String url) throws PendingRedirectException {
        
        Context cx = Context.enter();
        try {            
            PendingRedirectException pending = new PendingRedirectException(
                    (NativeContinuation) cx.captureContinuation().getContinuation());
            pending.setLocation(url);
            pending.setAllowCallbackResume(true);
            LOG.debug("Pausing flow");
            throw pending;
        } finally {
            Context.exit();
        }
    }
    
    public static boolean testEquality(Object a, Object b) {
    
        boolean anull = a == null;
        boolean bnull = b == null;
        
        // Same object?
        if (a == b) return true;
        if (!anull && !bnull) {
            
            Class aClass = a.getClass();
            Class bClass = b.getClass();
            if (!aClass.equals(bClass)) {
                
                //Native JS numbers land as double here
                if (aClass.equals(Double.class) && Number.class.isInstance(b)) {
                    return a.equals(((Number) b).doubleValue());
                    
                } else if (bClass.equals(Double.class) && Number.class.isInstance(a)) {
                    return b.equals(((Number) a).doubleValue());
                }
                
                LOG.warn("Trying to compare instances of {} and {}", aClass.getName(), bClass.getName());
                
                LogUtils.log("@w Equality check between % and % is not available",
                        simpleName(aClass), simpleName(bClass));
                
            } else if (aClass.equals(String.class) || PrimitiveUtils.isPrimitive(aClass, true)) {
                return a.equals(b);
            } else {
                LogUtils.log("@w Equality check is only effective for numbers, strings, and boolean values. " + 
                        "It returns false in other cases");
            }
        }
        return false;
        
    }
    
    //Issue a call to this method only if the request scope is active
    public static Pair<Function, NativeJavaObject> prepareSubflow(String qname, String parentBasepath,
        String[] pathOverrides) throws IOException {

        return CdiUtil.bean(FlowService.class).prepareSubflow(qname, parentBasepath, pathOverrides);
        
    }
    
    public static Object callAction(Object instance, String actionClassName, String methodName,
            Object[] params) throws Exception {
        
        return CdiUtil.bean(ActionService.class).callAction(instance, actionClassName, methodName, params);
        //TODO: remove?
        //if (Map.class.isInstance(value) && !NativeJavaMap.class.equals(value.getClass())) {
        //    Scriptable scope = CdiUtil.bean(FlowService.class).getGlobalScope();
        //    return new NativeJavaMap(scope, value);
        //}
        
    }

    //Issue a call to this method only if the request scope is active
    public static void closeSubflow() throws IOException {
        CdiUtil.bean(FlowService.class).closeSubflow();
    }
    
    private static String simpleName(Class<?> cls) {

        String name;
        if (List.class.isAssignableFrom(cls)) {
            name = "list";
        } else if (Map.class.isAssignableFrom(cls)) {
            name = "map";
        } else {
            name = cls.getSimpleName();
        }
        return name;
        
    }

}
