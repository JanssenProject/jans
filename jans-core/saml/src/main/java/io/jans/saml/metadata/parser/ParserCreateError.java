package io.jans.saml.metadata.parser;


public class ParserCreateError extends RuntimeException {

    public ParserCreateError(String msg){
        super(msg);
    }

    public ParserCreateError(String msg, Throwable cause) {
        super(msg,cause);
    }
}