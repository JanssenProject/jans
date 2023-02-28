package io.jans.configapi.core.protect;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Scope {

    @JsonProperty(value = "inum")
    private String inum;
    @JsonProperty(value = "name")
    private String  name;
    
    public String getInum() {
        return inum;
    }
    
    public void setInum(String inum) {
        this.inum = inum;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return "Scope [inum=" + inum + ", name=" + name + "]";
    }

   }
