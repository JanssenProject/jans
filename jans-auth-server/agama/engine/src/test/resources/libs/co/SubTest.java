package co;

public class SubTest extends Test {

    private String name;

    public SubTest() {}

    public SubTest(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
