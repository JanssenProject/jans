/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import static io.jans.scim.model.scim2.Constants.SEARCH_REQUEST_SCHEMA_ID;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class represents the components of a search request that is employed when doing searches via POST.
 * See section 3.4.3 RFC 7644.
 *
 * @author Val Pecaoco
 */
/*
 * Updated by jgomer on 2017-10-08.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchRequest {

    private List<String> schemas;
    private List<String> attributes;
    private List<String> excludedAttributes;
    private String filter;
    private String sortBy;
    private String sortOrder;
    private Integer startIndex;
    private Integer count;

    @JsonIgnore
    private String attributesStr;

    @JsonIgnore
    private String excludedAttributesStr;

    /**
     * Default no args constructor. It creates an instance of <code>SearchRequest</code> initializing {@link #getSchemas()
     * schemas} properly.
     */
    public SearchRequest() {
        schemas = Collections.singletonList(SEARCH_REQUEST_SCHEMA_ID);
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<String> schemas) {
        this.schemas = schemas;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    /**
     * Specifies a list of strings indicating the names of the resource attributes to return in response to a search,
     * overriding the set of attributes that would be returned by default.
     *
     * @param attributes A <code>List</code> of Strings
     */
    @JsonProperty
    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    /**
     * Specifies the names of the resource attributes to return in the response to a search, overriding the set of
     * attributes that would be returned by default.
     *
     * @param commaSeparatedString The attribute names in a comma-separated String
     */
    public void setAttributes(String commaSeparatedString) {
        setAttributes(commaSeparatedString == null ? null : Arrays.asList(commaSeparatedString.split(",")));
    }

    public List<String> getExcludedAttributes() {
        return excludedAttributes;
    }

    /**
     * Specifies a list of strings indicating the names of the resource attributes to be removed from the default set of
     * attributes to return.
     *
     * @param excludedAttributes A <code>List</code> of Strings
     */
    @JsonProperty
    public void setExcludedAttributes(List<String> excludedAttributes) {
        this.excludedAttributes = excludedAttributes;
    }

    /**
     * Specifies the names of the resource attributes to be removed from the default set of attributes to return.
     *
     * @param commaSeparatedString The attribute names in a comma-separated String
     */
    public void setExcludedAttributes(String commaSeparatedString) {
        setExcludedAttributes(commaSeparatedString == null ? null : Arrays.asList(commaSeparatedString.split(",")));
    }

    public String getFilter() {
        return filter;
    }

    /**
     * A filter expression so that the search will return only those resources matching the expression. To learn more
     * about SCIM filter expressions and operators, see section 3.4.2.2 of RFC 7644.
     *
     * @param filter A valid filter
     */
    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getSortBy() {
        return sortBy;
    }

    /**
     * Specifies the attribute whose value will be used to order the returned responses.
     *
     * @param sortBy Attribute name path. Examples are: <code>userName, name.givenName, emails.value</code>.
     */
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    /**
     * The order in which the <code>sortBy</code> parameter is applied. Allowed values are "ascending" and "descending",
     * being "ascending" the default if unspecified.
     *
     * @param sortOrder A string value
     */
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    /**
     * Sets the 1-based index of the first query result.
     *
     * @param startIndex Specifies "where" the result set will start when the search is performed
     */
    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    public Integer getCount() {
        return count;
    }

    /**
     * Specifies the desired maximum number of query results per page the response must include.
     *
     * @param count An <code>Integer</code> object
     */
    public void setCount(Integer count) {
        this.count = count;
    }

    public String getAttributesStr() {
        return attributes == null ? null : attributes.stream().collect(Collectors.joining(","));
    }

    public String getExcludedAttributesStr() {
        return excludedAttributes == null ? null : excludedAttributes.stream().collect(Collectors.joining(","));
    }

}
