package io.jans.as.model.configuration.rate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyExtractor {

    private KeySource source;
    private List<String> parameterNames = new ArrayList<>();

    public KeyExtractor() {
    }

    @JsonCreator
    public KeyExtractor(@JsonProperty("source") KeySource source, @JsonProperty("parameterNames") List<String> parameterNames) {
        setSource(source);
        setParameterNames(parameterNames);
    }

    @JsonProperty("source")
    public KeySource getSource() {
        return source;
    }

    @JsonProperty("source")
    public void setSource(KeySource source) {
        this.source = source;
    }

    @JsonProperty("parameterNames")
    public List<String> getParameterNames() {
        return parameterNames == null ? Collections.emptyList() : Collections.unmodifiableList(parameterNames);
    }

    @JsonProperty("parameterNames")
    public void setParameterNames(List<String> parameterNames) {
        // Defensive copy + filter null/blank
        List<String> safe = new ArrayList<>();
        if (parameterNames != null) {
            for (String p : parameterNames) {
                if (p == null) continue;
                String v = p.trim();
                if (!v.isEmpty()) safe.add(v);
            }
        }
        this.parameterNames = safe;
    }

    public boolean isWellFormed() {
        return source != null && !getParameterNames().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KeyExtractor)) return false;
        KeyExtractor that = (KeyExtractor) o;
        return source == that.source && Objects.equals(getParameterNames(), that.getParameterNames());
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, getParameterNames());
    }

    @Override
    public String toString() {
        return "KeyExtractor{" +
                "source=" + source +
                ", parameterNames=" + getParameterNames() +
                '}';
    }
}
