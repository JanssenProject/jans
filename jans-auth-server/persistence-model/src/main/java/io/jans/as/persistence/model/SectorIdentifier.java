/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.persistence.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2018
 */
@DataEntry(sortBy = {"id", "description"}, sortByName = {"jansId", "description"})
@ObjectClass(value = "jansSectorIdentifier")
public class SectorIdentifier extends BaseEntry implements Serializable {

    private static final long serialVersionUID = -2812480357430436514L;

    @AttributeName(name = "jansId", ignoreDuringUpdate = true)
    private String id;
    @NotNull
    @Size(min = 0, max = 250, message = "Length of the Description should not exceed 250")
    @AttributeName(name = "description")
    private String description;
    @AttributeName(name = "jansRedirectURI")
    private List<String> redirectUris;

    @AttributeName(name = "jansClntId")
    private List<String> clientIds;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getClientIds() {
        return clientIds;
    }

    public void setClientIds(List<String> clientIds) {
        this.clientIds = clientIds;
    }

    public String getDescription() {
        if (description == null) {
            description = "Default description";
        }
        return description;
    }

    public void setDescription(String des) {
        this.description = des;
    }

    @Override
    public String toString() {
        return String
                .format("OxAuthSectorIdentifier [id=%s, toString()=%s]",
                        id, super.toString());
    }
}
