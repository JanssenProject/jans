package co;

public class Test implements MyInterface {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static String sum(Test t1, Test t2) {
        if (t1 == null || t2 == null) return "";
        return t1.getId() + t2.getId();
    }

}