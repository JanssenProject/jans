package io.jans.service.document.store.exception;

public class WriteDocumentException extends RuntimeException{

    public WriteDocumentException() {
        super("Unable to process: document");
    }

    public WriteDocumentException(Throwable th) {
        super("Unable to process document", th);
    }

    public WriteDocumentException(String info) {
        super(info);
    }

    public WriteDocumentException(String info, Throwable th) {
        super(info, th);
    }
}

