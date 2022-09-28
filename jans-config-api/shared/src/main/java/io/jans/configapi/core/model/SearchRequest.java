package io.jans.configapi.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchRequest {

    private String schemas;
    private List<String> attributes;
    private List<String> excludedAttributes;
    private String filter;
    private String sortBy;
    private String sortOrder;
    private Integer startIndex;
    private Integer count;
    private int maxCount;
    private List<String> filterAttributeName;
    private List<String> filterAssertionValue;

    @JsonIgnore
    private String attributesStr;

    @JsonIgnore
    private String excludedAttributesStr;

    public String getSchemas() {
        return schemas;
    }

    public void setSchemas(String schemas) {
        this.schemas = schemas;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public void setAttributes(String commaSeparatedString) {
        setAttributes(commaSeparatedString == null ? null : Arrays.asList(commaSeparatedString.split(",")));
    }

    public List<String> getExcludedAttributes() {
        return excludedAttributes;
    }

    public void setExcludedAttributes(List<String> excludedAttributes) {
        this.excludedAttributes = excludedAttributes;
    }

    public void setExcludedAttributes(String commaSeparatedString) {
        setExcludedAttributes(commaSeparatedString == null ? null : Arrays.asList(commaSeparatedString.split(",")));
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getAttributesStr() {
        return attributes == null ? null : attributes.stream().collect(Collectors.joining(","));
    }

    public void setAttributesStr(String attributesStr) {
        this.attributesStr = attributesStr;
    }

    public String getExcludedAttributesStr() {
        return excludedAttributes == null ? null : excludedAttributes.stream().collect(Collectors.joining(","));
    }

    public void setExcludedAttributesStr(String excludedAttributesStr) {
        this.excludedAttributesStr = excludedAttributesStr;
    }

    public int getMaxCount() {
        return this.maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public List<String> getFilterAttributeName() {
        return filterAttributeName;
    }

    public void setFilterAttributeName(List<String> filterAttributeName) {
        this.filterAttributeName = filterAttributeName;
    }

    public List<String> getFilterAssertionValue() {
        return filterAssertionValue;
    }

    public void setFilterAssertionValue(List<String> filterAssertionValue) {
        this.filterAssertionValue = filterAssertionValue;
    }

    @Override
    public String toString() {
        return "SearchRequest [schemas=" + schemas + ", attributes=" + attributes + ", excludedAttributes="
                + excludedAttributes + ", filter=" + filter + ", sortBy=" + sortBy + ", sortOrder=" + sortOrder
                + ", startIndex=" + startIndex + ", count=" + count + ", maxCount=" + maxCount
                + ", filterAttributeName=" + filterAttributeName + ", filterAssertionValue=" + filterAssertionValue
                + ", attributesStr=" + attributesStr + ", excludedAttributesStr=" + excludedAttributesStr + "]";
    }
}
