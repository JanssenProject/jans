/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.util.AuthUtil;
import io.jans.orm.model.AttributeData;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.configuration.ConfigurationFactory;

import jakarta.inject.Inject;

import java.util.*;

import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */
public class ConfigBaseResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    ConfigurationFactory configurationFactory;
    
    @Inject
    AuthUtil authUtil;

    protected int getMaxCount() {
        logger.trace(" MaxCount details - ApiAppConfiguration.MaxCount():{}, ApiConstants.DEFAULT_MAX_COUNT:{} ",
                configurationFactory.getApiAppConfiguration().getMaxCount(), ApiConstants.DEFAULT_MAX_COUNT);
        return (configurationFactory.getApiAppConfiguration().getMaxCount() > 0
                ? configurationFactory.getApiAppConfiguration().getMaxCount()
                : ApiConstants.DEFAULT_MAX_COUNT);
    }
    
    public <T> List<AttributeData> getAttributeData(String dn, String objectClass) {
        logger.error("AttributeData details to be fetched for dn:{}, objectClass:{} ", dn, objectClass);
        List<AttributeData> attributeDataList = authUtil.getAttributeData(dn, objectClass);
        logger.error("AttributeData details fetched for dn:{}, objectClass:{} is attributeDataList:{}", dn, objectClass, attributeDataList);
        return attributeDataList;
    }

    public <T> Map<String,String> getAttributeData(String dn, String objectClass, Map<String,String> fieldValueMap) {
        logger.error("AttributeData details to be fetched for dn:{}, objectClass:{}, fieldValueMap:{} ", dn, objectClass, fieldValueMap);
        List<AttributeData> attributeDataList = getAttributeData(dn, objectClass);
        logger.error("AttributeData details to be fetched for attributeDataList:{} ", attributeDataList);
        return fieldValueMap;
    }

}
