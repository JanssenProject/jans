package com.acme.agama.survey;

import java.util.*;

public class Answer {

    private String id;
    private List<String> choice;
    private String text;
    
    public Answer() { }
    
    public Answer(String id, List<String> choice, String text) {
        setId(id);
        setChoice(choice);
        setText(text);
    }
    
    public String getId() {
        return id;
    }
    
    public List<String> getChoice() {
        return choice;
    }
    
    public String getText() {
        return text;
    }
 
    public void setId(String id) {
        this.id = id;
    }
    
    public void setChoice(List<String> choice) {
        this.choice = choice;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String toString() {
        return "{ id: " + Objects.toString(id) + ", choice: " + Objects.toString(choice) +
               ", text: " + Objects.toString(text) + "}";
    }

}
