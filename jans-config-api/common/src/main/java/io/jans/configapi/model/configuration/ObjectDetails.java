package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectDetails implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Schema(description = "Name of the object.")
    private String name;

    @Schema(description = "List of string text that is to be ignored .")
    private List<String> text;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getText() {
        return text;
    }

    public void setText(List<String> text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "ObjectDetails [name=" + name + ", text=" + text + "]";
    }

}
