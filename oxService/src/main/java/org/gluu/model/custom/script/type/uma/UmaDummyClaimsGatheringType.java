package org.gluu.model.custom.script.type.uma;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;

/**
 * @author yuriyz on 06/16/2017.
 */
public class UmaDummyClaimsGatheringType implements UmaClaimsGatheringType {

    @Override
    public boolean gather(int step, Object gatheringContext) {
        return false;
    }

    @Override
    public int getNextStep(int step, Object gatheringContext) {
        return -1;
    }

    @Override
    public boolean prepareForStep(int step, Object gatheringContext) {
        return false;
    }

    @Override
    public int getStepsCount(Object gatheringContext) {
        return -1;
    }

    @Override
    public String getPageForStep(int step, Object gatheringContext) {
        return null;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public int getApiVersion() {
        return 1;
    }

}
