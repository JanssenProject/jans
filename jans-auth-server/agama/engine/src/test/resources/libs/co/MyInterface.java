package co;

public interface MyInterface {
    
    default String echo(String s) {
        return s;
    }

}
