package io.jans.ads.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

import java.util.Date;

@DataEntry
@ObjectClass(value = "adsPrjDeployment")
public class Deployment extends Entry {
    
    public static final String ASSETS_ATTR = "adsPrjAssets";

    @AttributeName(name = "jansId")
    private String id;
    
    @AttributeName(name = "jansStartDate")
    private Date createdAt;

    @AttributeName(name = "jansActive")
    private boolean taskActive;
    
    @AttributeName(name = "jansEndDate")
    private Date finishedAt;
    
    @AttributeName(name = Deployment.ASSETS_ATTR)
    private String assets;

    @JsonObject
    @AttributeName(name = "adsPrjDeplDetails")
    private DeploymentDetails details;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isTaskActive() {
        return taskActive;
    }

    public void setTaskActive(boolean taskActive) {
        this.taskActive = taskActive;
    }

    public Date getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Date finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getAssets() {
        return assets;
    }

    public void setAssets(String assets) {
        this.assets = assets;
    }

    public DeploymentDetails getDetails() {
        return details;
    }

    public void setDetails(DeploymentDetails details) {
        this.details = details;
    }
    
}