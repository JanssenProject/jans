package org.gluu.configapi.exception;

public class GlobalRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public GlobalRuntimeException(Throwable cause) {
      super(cause);
  }
}