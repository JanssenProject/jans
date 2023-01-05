package io.jans.agama.dsl.error;

import java.util.Optional;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class RecognitionErrorListener extends BaseErrorListener {

    private SyntaxException error;

    public SyntaxException getError() {
        return error;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
            int charPositionInLine, String msg, RecognitionException exception) {

        if (error == null) {
            error = new SyntaxException(msg,
                    Optional.ofNullable(offendingSymbol).map(Object::toString)
                            .orElse(null), line, charPositionInLine);
        }

    }

}
