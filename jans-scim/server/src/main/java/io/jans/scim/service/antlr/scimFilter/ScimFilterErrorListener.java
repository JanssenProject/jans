/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.antlr.scimFilter;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * Created by jgomer on 2017-12-09.
 */
public class ScimFilterErrorListener extends BaseErrorListener {

    private String output;
    private String symbol;

    public String getOutput() {
        return output;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException exception){
        output=msg;
        symbol=offendingSymbol==null ? null : offendingSymbol.toString();
    }

}
