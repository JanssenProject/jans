/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.jans.model.GluuStatus;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;
import io.jans.configapi.core.model.ValidationStatus;
import io.swagger.v3.oas.annotations.Hidden;


import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "jansRealm")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Realm extends Entry implements Serializable {

    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @NotNull
    @AttributeName(name = "name")
    private String name;

    @NotNull
    @Size(min = 0, max = 60, message = "Length of the Display Name should not exceed 60")
    @AttributeName
    private String displayName;

	
    @AttributeName(name = "jansEnabled")
    private boolean enabled;


    public String getInum() {
        return inum;
    }


    public void setInum(String inum) {
        this.inum = inum;
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public String getDisplayName() {
        return displayName;
    }


    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }


    public boolean isEnabled() {
        return enabled;
    }


    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


      
}
