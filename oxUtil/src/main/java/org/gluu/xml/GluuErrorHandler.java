/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.xml;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * @author Oleksiy Tataryn
 */
public class GluuErrorHandler implements ErrorHandler {

    public static final String VALIDATOR_INTERNAL_ERROR_MESSAGE = "Internal error of validator";
    public static final String SCHEMA_CREATING_ERROR_MESSAGE = "Error of schema creating";

    private boolean result = true;
    private boolean internalError = false;
    private final List<String> validationLog;

    public GluuErrorHandler() {
        validationLog = new ArrayList<String>();
    }

    public GluuErrorHandler(boolean result, boolean internalError, List<String> validationLog) {
        this.result = result;
        this.internalError = internalError;
        this.validationLog = validationLog;
    }

    public boolean isValid() {
        return result;
    }

    public boolean isInternalError() {
        return internalError;
    }

    public List<String> getLog() {
        return validationLog.isEmpty() ? null : validationLog;
    }

    public void error(SAXParseException arg0) {
        result = false;
        validationLog.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                + " : ERROR : " + arg0.getMessage());
    }

    public void fatalError(SAXParseException arg0) {
        result = false;
        validationLog.add(new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                + " : FATAL : " + arg0.getMessage());
    }

    public void warning(SAXParseException arg0) {
        validationLog.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                + " : WARNING :" + arg0.getMessage());
    }

}
