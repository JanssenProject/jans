package co.coo

class Foo {
    
    String name
    
    void bar() {
        //Intentionally malformed
        "Hello ${name}        
    }

}