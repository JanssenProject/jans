package io.jans.service.document.store.exception;

public class DocumentException extends RuntimeException{

    public DocumentException() {
        super("Unable to process: document");
    }

    public DocumentException(Throwable th) {
        super("Unable to process document", th);
    }

    public DocumentException(String info) {
        super(info);
    }

    public DocumentException(String info, Throwable th) {
        super(info, th);
    }
}

