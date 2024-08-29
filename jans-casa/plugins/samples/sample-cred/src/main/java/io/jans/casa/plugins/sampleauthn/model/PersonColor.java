package io.jans.casa.plugins.sampleauthn.model;

import io.jans.casa.core.model.BasePerson;
import io.jans.orm.annotation.*;

@DataEntry
@ObjectClass("jansPerson")
public class PersonColor extends BasePerson {

    //To avoid extra configurations, we use the 'secretAnswer' database attribute to
    //store the user's favorite color. In practice you should create your own attribute
    @AttributeName(name = "secretAnswer")
    private String color;

    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }

}
