package io.jans.service.custom.script;

import io.jans.exception.PythonException;
import io.jans.model.ProgrammingLanguage;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.model.ScriptError;
import io.jans.model.custom.script.type.BaseExternalType;
import io.jans.service.PythonService;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.openhft.compiler.CompilerUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.io.IoBuilder;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class ExternalTypeCreator {

    private static final AtomicBoolean classPathInitialized = new AtomicBoolean(false);

    @Inject
    protected Logger log;

    @Inject
    protected PythonService pythonService;

    @Inject
    protected AbstractCustomScriptService customScriptService;

    private void initJavaCompilerClasspathIfNeeded() {
        if (classPathInitialized.get()) {
            return;
        }
        try {
            classPathInitialized.set(true);

            URL[] urls = ((URLClassLoader) this.getClass().getClassLoader()).getURLs();
            for (URL url : urls) {
                CompilerUtils.addClassPath(url.getFile());
            }
        } catch (Exception e) {
            log.error("FAILED to output class loader urls", e);
        }
    }

    public BaseExternalType createExternalType(CustomScript customScript,
                                               Map<String, SimpleCustomProperty> configurationAttributes) {
        String customScriptInum = customScript.getInum();

        BaseExternalType externalType;
        try {
            if (customScript.getProgrammingLanguage() == ProgrammingLanguage.JAVA) {
                externalType = createExternalTypeWithJava(customScript);
            } else {
                externalType = createExternalTypeFromStringWithPythonException(customScript);
            }
        } catch (Exception ex) {
            log.error("Failed to prepare external type '{}', exception: '{}'", customScriptInum, ExceptionUtils.getStackTrace(ex));
            log.error("Script '{}'", customScript.getScript());
            saveScriptError(customScript, ex, true);
            return null;
        }

        externalType = initExternalType(externalType, customScript, configurationAttributes);

        if (externalType == null) {
            log.debug("Using default external type class");
            saveScriptError(customScript, new Exception("Using default external type class"), true);
            externalType = customScript.getScriptType().getDefaultImplementation();
        } else {
            clearScriptError(customScript);
        }

        return externalType;
    }

    private BaseExternalType initExternalType(BaseExternalType externalType, CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        if (externalType == null) {
            return null;
        }

        boolean initialized = false;
        try {
            if (externalType.getApiVersion() > 10) {
                initialized = externalType.init(customScript, configurationAttributes);
            } else {
                initialized = externalType.init(configurationAttributes);
                log.warn(" Update the script's init method to init(self, customScript, configurationAttributes), script name: {}", customScript.getName());
            }
        } catch (Exception ex) {
            log.error("Failed to initialize custom script: '{}', exception: {}", customScript.getName(), ex);
        }

        if (initialized) {
            return externalType;
        }
        return null;
    }

    private BaseExternalType createExternalTypeWithJava(CustomScript customScript) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        initJavaCompilerClasspathIfNeeded();
        CustomScriptType customScriptType = customScript.getScriptType();

        PrintWriter printWriter = IoBuilder.forLogger(getClass()).setLevel(Level.DEBUG).buildPrintWriter();
        Class<?> aClass = CompilerUtils.CACHED_COMPILER.loadFromJava(getClass().getClassLoader(), customScriptType.getClassName(), customScript.getScript(), printWriter);
        return (BaseExternalType) aClass.getDeclaredConstructor().newInstance();
    }

    public BaseExternalType createExternalTypeFromStringWithPythonException(CustomScript customScript) throws PythonException, IOException {
        String script = customScript.getScript();
        String scriptName = StringHelper.toLowerCase(customScript.getName()) + ".py";
        if (script == null) {
            return null;
        }

        CustomScriptType customScriptType = customScript.getScriptType();

        try (InputStream bis = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8))) {
            return pythonService.loadPythonScript(bis, scriptName, customScriptType.getClassName(),
                    customScriptType.getCustomScriptType(), new PyObject[]{new PyLong(System.currentTimeMillis())});
        }
    }

    public void saveScriptError(CustomScript customScript, Exception exception) {
        saveScriptError(customScript, exception, false);
    }

    public void saveScriptError(CustomScript customScript, Exception exception, boolean overwrite) {
        try {
            saveScriptErrorImpl(customScript, exception, overwrite);
        } catch (Exception ex) {
            log.error("Failed to store script '{}' error", customScript.getInum(), ex);
        }
    }

    protected void saveScriptErrorImpl(CustomScript customScript, Exception exception, boolean overwrite) {
        // Load entry from DN
        String customScriptDn = customScript.getDn();
        Class<? extends CustomScript> scriptType = customScript.getScriptType().getCustomScriptModel();
        CustomScript loadedCustomScript = customScriptService.getCustomScriptByDn(scriptType, customScriptDn);

        // Check if there is error value already
        ScriptError currError = loadedCustomScript.getScriptError();
        if (!overwrite && (currError != null)) {
            return;
        }

        // Save error into script entry
        StringBuilder builder = new StringBuilder();
        builder.append(ExceptionUtils.getStackTrace(exception));
        String message = exception.getMessage();
        if (message != null && !StringUtils.isEmpty(message)) {
            builder.append("\n==================Further details============================\n");
            builder.append(message);
        }
        loadedCustomScript.setScriptError(new ScriptError(new Date(), builder.toString()));
        customScriptService.update(loadedCustomScript);
    }

    public void clearScriptError(CustomScript customScript) {
        try {
            clearScriptErrorImpl(customScript);
        } catch (Exception ex) {
            log.error("Failed to clear script '{}' error", customScript.getInum(), ex);
        }
    }

    protected void clearScriptErrorImpl(CustomScript customScript) {
        // Load entry from DN
        String customScriptDn = customScript.getDn();
        Class<? extends CustomScript> scriptType = customScript.getScriptType().getCustomScriptModel();
        CustomScript loadedCustomScript = customScriptService.getCustomScriptByDn(scriptType, customScriptDn);

        // Check if there is no error
        ScriptError currError = loadedCustomScript.getScriptError();
        if (currError == null) {
            return;
        }

        // Save error into script entry
        loadedCustomScript.setScriptError(null);
        customScriptService.update(loadedCustomScript);
    }
}
