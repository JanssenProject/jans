package io.jans.saml.metadata.parser;


public class ParseError extends  RuntimeException {


    public ParseError(String msg) {
        super(msg);
    }

    public ParseError(String msg, Throwable cause) {
        super(msg,cause);
    }
}