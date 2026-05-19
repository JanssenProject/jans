package io.jans.common;


public class Result<T> {

    private final T value;
    private final Object error;
    private final Boolean success;

    protected Result(T value, Object error, boolean success) {

        this.value = value;
        this.error = error;
        this.success = success;
    }

    public static <T> Result<T> success (T value) {

        return new Result<>(value,null,true);
    }

    public static <T> Result<T> failure(Object error) {

        return new Result<>(null,error,false);
    }

    public boolean isSuccess() { 

        return success; 
    }

    public boolean isFailure() {

        return !isSuccess();
    }

    public T getValue() {

        if(!success) {

            throw new IllegalStateException("Cannot get value from failed result with error '" + error.toString() + "'");
        }
        return value;
    }

    public Object getError() {

        if (success) {
            throw new IllegalStateException("Cannot get error from successful result");
        }
        return error;
    }
}