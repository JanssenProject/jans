package org.gluu.model.custom.script.type.uma;

import org.gluu.model.custom.script.type.BaseExternalType;

/**
 * @author yuriyz on 06/16/2017.
 */
public interface UmaClaimsGatheringType extends BaseExternalType {

    boolean gather(int step, Object gatheringContext);

    int getNextStep(int step, Object gatheringContext);

    boolean prepareForStep(int step, Object gatheringContext);

    int getStepsCount(Object gatheringContext);

    String getPageForStep(int step, Object gatheringContext);
}
