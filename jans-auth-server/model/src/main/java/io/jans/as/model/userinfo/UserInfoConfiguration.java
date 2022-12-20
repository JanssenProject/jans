package io.jans.as.model.userinfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.doc.annotation.DocProperty;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class UserInfoConfiguration {

    @DocProperty(description = "List of key value, e.g. 'birthdate: 'yyyy-MM-dd', etc.")
    private Map<String, String> dateFormatterPattern = new HashMap<>();

    public Map<String, String> getDateFormatterPattern() {
        return dateFormatterPattern;
    }

    public void setDateFormatterPattern(Map<String, String> dateFormatterPattern) {
        this.dateFormatterPattern = dateFormatterPattern;
    }
}
