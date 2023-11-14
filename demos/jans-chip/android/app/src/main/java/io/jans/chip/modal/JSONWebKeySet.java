package io.jans.chip.modal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JSONWebKeySet {

    public JSONWebKeySet(List<Map<String, ?>> keys) {
        this.keys = keys;
    }

    private List<Map<String, ?>> keys;

    public JSONWebKeySet() {
        keys = new ArrayList<>();
    }

    public List<Map<String, ?>> getKeys() {
        return keys;
    }

    public void setKeys(List<Map<String, ?>> keys) {
        this.keys = keys;
    }

    public void addKey(Map<String, ?> key){
        keys.add(key);
    }

    public String toJsonString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
