/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.model.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.StringUtils;

import io.jans.orm.annotation.AttributeName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Name {

    @AttributeName(name = "familyName")
    private String familyName;

    @AttributeName(name = "givenName")
    private String givenName;

    @AttributeName(name = "middleName")
    private String middleName;

    @AttributeName(name = "honorificPrefix")
    private String honorificPrefix;

    @AttributeName(name = "honorificSuffix")
    private String honorificSuffix;

    @AttributeName(name = "formatted")
    private String formatted;

    /**
     * From this Name instance, it builds a string depicting a full name including all middle names, titles, and suffixes
     * as appropriate for display if the {@link #getFormatted() formatted} field of this object is null or empty
     * @return A string representing a full name. The formatted field will be set to this value
     */
    public String computeFormattedName(){

        if (StringUtils.isEmpty(formatted)) {
            String formattedName = "";

            formattedName+=StringUtils.isEmpty(honorificPrefix) ? "" : honorificPrefix + " ";
            formattedName+=StringUtils.isEmpty(givenName) ? "" : givenName + " ";
            formattedName+=StringUtils.isEmpty(middleName) ? "" : middleName + " ";
            formattedName+=StringUtils.isEmpty(familyName) ? "" : familyName + " ";
            formattedName+=StringUtils.isEmpty(honorificSuffix) ? "" : honorificSuffix;
            formattedName=formattedName.trim();

            formatted=formattedName.length()==0 ? null : formattedName;
        }
        return formatted;

    }

    public String getFormatted() {
        return formatted;
    }

    public void setFormatted(String formatted) {
        this.formatted = formatted;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getHonorificPrefix() {
        return honorificPrefix;
    }

    public void setHonorificPrefix(String honorificPrefix) {
        this.honorificPrefix = honorificPrefix;
    }

    public String getHonorificSuffix() {
        return honorificSuffix;
    }

    public void setHonorificSuffix(String honorificSuffix) {
        this.honorificSuffix = honorificSuffix;
    }

}
