package io.jans.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldFilterData {

    private String field;
    private String operator;
    private String value;
    
    public FieldFilterData() {}
    
    public FieldFilterData(String field, String operator, String value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
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

    @Override
    public String toString() {
        return "FieldMap [field:" + field + ", operator:" + operator + ", value:" + value + "]";
    }

}
