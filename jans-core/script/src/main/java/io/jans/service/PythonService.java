/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.exception.PythonException;
import io.jans.orm.reflect.util.ReflectHelper;
import io.jans.util.StringHelper;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides operations with python module
 *
 * @author Yuriy Movchan Date: 08.21.2012
 */
@ApplicationScoped
public class PythonService implements Serializable {

    private static final long serialVersionUID = 3398422090669045605L;

    @Inject
    private Logger log;

    private PythonInterpreter pythonInterpreter;
    private boolean interpereterReady;

    private OutputStreamWriter logOut, logErr;

    @PostConstruct
    public void init() {
        try {
            this.logOut = new OutputStreamWriter(new PythonLoggerOutputStream(log, false), "UTF-8");
            this.logErr = new OutputStreamWriter(new PythonLoggerOutputStream(log, true), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            log.error("Failed to initialize Jython out/err loggers", ex);
        }
    }

    public void configure() {
		this.log = LoggerFactory.getLogger(PythonService.class);
	}

    /*
     * Initialize singleton instance during startup
     */
    public boolean initPythonInterpreter(String pythonModulesDir) {
        boolean result = false;

        String pythonHome = getPythonHome();
    	log.info("Initializing PythonService with Jython: '{}'", pythonHome);
        if (StringHelper.isNotEmpty(pythonHome)) {
            try {

            	PythonInterpreter.initialize(getPreProperties(), getPostProperties(pythonModulesDir, pythonHome), null);
                this.pythonInterpreter = new PythonInterpreter();

                initPythonInterpreter(this.pythonInterpreter);

                result = true;
            } catch (PyException ex) {
                log.error("Failed to initialize PythonInterpreter correctly", ex);
            } catch (Exception ex) {
                log.error("Failed to initialize PythonInterpreter correctly", ex);
            }
        } else {
        	log.error("Failed to initialize PythonService. Jython location is not defined!");
        }

        this.interpereterReady = result;

        return result;
    }

    private void initPythonInterpreter(PythonInterpreter interpreter) {
        // Init output redirect interpreter
        if (this.logOut != null) {
            interpreter.setOut(this.logOut);
        }

        if (this.logErr != null) {
            interpreter.setErr(this.logErr);
        }
    }

    /**
     * When application undeploy we need clean up pythonInterpreter
     */
    @PreDestroy
    public void destroy() {
        log.debug("Destroying pythonInterpreter component");
        if (this.pythonInterpreter != null) {
            this.pythonInterpreter.cleanup();
        }
    }

    private Properties getPreProperties() {
        Properties props = System.getProperties();
        Properties clonedProps = (Properties) props.clone();
        clonedProps.setProperty("java.class.path", ".");
        clonedProps.setProperty("java.library.path", "");
        clonedProps.remove("javax.net.ssl.trustStore");
        clonedProps.remove("javax.net.ssl.trustStorePassword");

        return clonedProps;
    }

    private Properties getPostProperties(String pythonModulesDir, String pythonHome) {
        Properties props = getPreProperties();

        String catalinaTmpFolder = System.getProperty("java.io.tmpdir") + File.separator + "python" + File.separator + "cachedir";
        props.setProperty("python.cachedir", catalinaTmpFolder);

        props.setProperty("python.home", pythonHome);

        // Register custom python modules
        if (StringHelper.isNotEmpty(pythonModulesDir)) {
            props.setProperty("python.path", pythonModulesDir);
        }

        props.put("python.console.encoding", "UTF-8");
        props.put("python.import.site", "false");

        return props;
    }

	private String getPythonHome() {
		String pythonHome = System.getenv("PYTHON_HOME");
        if (StringHelper.isNotEmpty(pythonHome)) {
            return pythonHome;
        }

        return System.getProperty("python.home");
	}

    public <T> T loadPythonScript(String scriptName, String scriptPythonType, Class<T> scriptJavaType, PyObject[] constructorArgs)
            throws PythonException {
        if (!interpereterReady || StringHelper.isEmpty(scriptName)) {
            return null;
        }

        PythonInterpreter currentPythonInterpreter = PythonInterpreter.threadLocalStateInterpreter(null);
        initPythonInterpreter(currentPythonInterpreter);

        try {
            currentPythonInterpreter.execfile(scriptName);
        } catch (Exception ex) {
            log.error("Failed to load python file", ex.getMessage());
            throw new PythonException(String.format("Failed to load python file '%s'", scriptName), ex);
        }

        return loadPythonScript(scriptPythonType, scriptJavaType, constructorArgs, currentPythonInterpreter);
    }

    public <T> T loadPythonScript(InputStream scriptFile, String scriptName, String scriptPythonType, Class<T> scriptJavaType,
            PyObject[] constructorArgs) throws PythonException {
        if (!interpereterReady || (scriptFile == null)) {
            return null;
        }

        PythonInterpreter currentPythonInterpreter = PythonInterpreter.threadLocalStateInterpreter(null);
        initPythonInterpreter(currentPythonInterpreter);

        try {
            currentPythonInterpreter.execfile(scriptFile, scriptName);
        } catch (Exception ex) {
            log.error("Failed to load python file" + ex.getMessage(), ex);
            throw new PythonException(String.format("Failed to load python file '%s'", scriptFile), ex);
        }

        return loadPythonScript(scriptPythonType, scriptJavaType, constructorArgs, currentPythonInterpreter);
    }

    @SuppressWarnings("unchecked")
    private <T> T loadPythonScript(String scriptPythonType, Class<T> scriptJavaType, PyObject[] constructorArgs, PythonInterpreter interpreter)
            throws PythonException {
        PyObject scriptPythonTypeObject = interpreter.get(scriptPythonType);
        if (scriptPythonTypeObject == null) {
            return null;
        }

        PyObject scriptPythonTypeClass;
        try {
            scriptPythonTypeClass = scriptPythonTypeObject.__call__(constructorArgs);
        } catch (Exception ex) {
            log.error("Failed to initialize python class", ex.getMessage());
            throw new PythonException(String.format("Failed to initialize python class '%s'", scriptPythonType), ex);
        }

        Object scriptJavaClass = scriptPythonTypeClass.__tojava__(scriptJavaType);
        if (!ReflectHelper.assignableFrom(scriptJavaClass.getClass(), scriptJavaType)) {
            return null;

        }

        return (T) scriptJavaClass;
    }

    final class PythonLoggerOutputStream extends OutputStream {

        private boolean error;
        private Logger log;
        private StringBuffer buffer;

        private PythonLoggerOutputStream(Logger log, boolean error) {
            this.error = error;
            this.log = log;
            this.buffer = new StringBuffer();
        }

        public void write(int b) throws IOException {
            if (((char) b == '\n') || ((char) b == '\r')) {
                flush();
            } else {
                buffer.append((char) b);
            }
        }

        public void flush() {
            if (buffer.length() > 0) {
                if (error) {
                    this.log.error(buffer.toString());
                } else {
                    this.log.info(buffer.toString());
                }

                this.buffer.setLength(0);
            }
        }
    }

}
