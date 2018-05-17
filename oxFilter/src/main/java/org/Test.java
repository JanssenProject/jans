package org;
import java.util.Properties;

public class Test {
    
    public static void main(String[] args) {
        Properties prop = new Properties();
        
        prop.put("servers", "value, value");
        
        System.out.println(prop);
        System.out.println(prop.getProperty("servers").split(",")[1]);
    }

}
