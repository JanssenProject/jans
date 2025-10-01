package io.jans.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldFilterData {

    private String field;
    private String operator;
    private String value;
    private String type;
    
    public FieldFilterData() {}
    
    public FieldFilterData(String field, String operator, String value, String type) {
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.type = type;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "FieldFilterData [field:" + field + ", operator:" + operator + ", value:" + value + ", type:" + type
                + "]";
    }
    
}
