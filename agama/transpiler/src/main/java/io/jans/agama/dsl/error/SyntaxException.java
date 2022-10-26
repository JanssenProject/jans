package io.jans.agama.dsl.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Optional;
import java.util.function.Function;

@JsonIgnoreProperties( { "cause", "localizedMessage", "stackTrace", "suppressed" } )
public class SyntaxException extends Exception {

    private String error;
    private String symbol;
    private int line;
    private int column;

    public SyntaxException() {
    }
    
    public SyntaxException(String error, String symbol, int line, int column) {
        this.error = error;
        this.symbol = symbol;
        this.line = line;
        this.column = column;
    }
    
    @Override
    public String getMessage() {
        
        String msg = "Syntax error: ";
        msg += Optional.ofNullable(error).map(Function.identity()).orElse("");
        msg += Optional.ofNullable(symbol).map(s -> String.format("\nSymbol: %s", s)).orElse("");
        msg += String.format("\nLine: %d", line);
        msg += String.format("\nColumn: %d", column + 1);
        return msg;

    }

    /**
     * @return the error
     */
    public String getError() {
        return error;
    }

    /**
     * @return the symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * @return the line
     */
    public int getLine() {
        return line;
    }

    /**
     * @return the column
     */
    public int getColumn() {
        return column;
    }
    
}
